#!/usr/bin/env python3
"""Idempotently append validated model links to their existing Notion pages."""

from __future__ import annotations

import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Callable


NOTION_API = "https://api.notion.com/v1"
NOTION_API_VERSION = "2022-06-28"


class NotionSyncError(RuntimeError):
    """Raised when an existing Notion page cannot be safely synchronized."""


def canonical_page_id(page_id: str) -> str:
    compact = page_id.replace("-", "").lower()
    if not re.fullmatch(r"[0-9a-f]{32}", compact):
        raise NotionSyncError("The exported Notion page ID is invalid.")
    return (
        f"{compact[:8]}-{compact[8:12]}-{compact[12:16]}-"
        f"{compact[16:20]}-{compact[20:]}"
    )


def model_url(base_url: str, creature_id: str) -> str:
    parsed = urllib.parse.urlsplit(base_url)
    if (
        parsed.scheme != "https"
        or not parsed.netloc
        or parsed.username
        or parsed.password
        or parsed.query
        or parsed.fragment
    ):
        raise NotionSyncError(
            "The asset base URL must be HTTPS without credentials, query, or fragment."
        )
    return urllib.parse.urljoin(
        base_url.rstrip("/") + "/",
        urllib.parse.quote(f"{creature_id}/model.glb", safe="/-"),
    )


def verify_asset_url(
    url: str,
    opener: Callable[..., Any] = urllib.request.urlopen,
) -> None:
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != "https" or not parsed.netloc or parsed.username:
        raise NotionSyncError("The model asset must be reachable over HTTPS.")
    for method in ("HEAD", "GET"):
        headers = {"User-Agent": "Ruff-3D-asset-check"}
        if method == "GET":
            headers["Range"] = "bytes=0-0"
        request = urllib.request.Request(url, method=method, headers=headers)
        try:
            with opener(request, timeout=15) as response:
                status = response.status
                final_url = urllib.parse.urlsplit(response.geturl())
                if final_url.scheme != "https":
                    raise NotionSyncError(
                        "The model asset redirected to a non-HTTPS URL."
                    )
                if 200 <= status < 300:
                    return
                raise NotionSyncError(
                    f"The model asset returned HTTP {status}."
                )
        except urllib.error.HTTPError as error:
            if method == "HEAD" and error.code in {405, 501}:
                error.close()
                continue
            error.close()
            raise NotionSyncError(
                f"The model asset returned HTTP {error.code}."
            ) from None
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            raise NotionSyncError(
                f"The model asset could not be reached: {type(error).__name__}."
            ) from None
    raise NotionSyncError("The model asset does not support a reachability check.")


class NotionClient:
    def __init__(
        self,
        token: str,
        api_version: str = NOTION_API_VERSION,
        opener: Callable[..., Any] | None = None,
        sleep: Callable[[float], None] = time.sleep,
        max_retries: int = 3,
    ) -> None:
        if not token:
            raise NotionSyncError("NOTION_TOKEN is not configured.")
        self._token = token
        self._api_version = api_version
        self._opener = opener or urllib.request.urlopen
        self._sleep = sleep
        self._max_retries = max_retries

    def request(
        self,
        method: str,
        endpoint: str,
        body: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(
            f"{NOTION_API}{endpoint}",
            data=data,
            method=method,
            headers={
                "Authorization": f"Bearer {self._token}",
                "Notion-Version": self._api_version,
                "Content-Type": "application/json",
            },
        )
        for attempt in range(self._max_retries + 1):
            try:
                with self._opener(request, timeout=20) as response:
                    payload = json.loads(response.read().decode("utf-8"))
                    if not isinstance(payload, dict):
                        raise NotionSyncError("Notion returned an unexpected response.")
                    return payload
            except urllib.error.HTTPError as error:
                try:
                    error_body = error.read(4096)
                finally:
                    error.close()
                blocked = b"public_api_request_blocked" in error_body
                retryable_status = (
                    (error.code in {429, 529} and not blocked)
                    or error.code in {500, 502, 503, 504}
                )
                retryable = retryable_status and (
                    method.upper() in {"GET", "HEAD"}
                    or (error.code == 429 and not blocked)
                )
                if retryable and attempt < self._max_retries:
                    retry_after = error.headers.get("Retry-After", "1")
                    try:
                        delay = max(float(retry_after), 0.0)
                    except ValueError:
                        delay = 1.0
                    self._sleep(delay)
                    continue
                raise NotionSyncError(
                    f"Notion API request failed with HTTP {error.code}."
                ) from None
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
                if attempt < self._max_retries:
                    self._sleep(min(2**attempt, 8))
                    continue
                raise NotionSyncError(
                    f"Notion API request failed: {type(error).__name__}."
                ) from None
        raise NotionSyncError("Notion API request exhausted its retry limit.")

    def _has_marker(self, page_id: str, marker: str) -> bool:
        cursor: str | None = None
        while True:
            query = {"page_size": "100"}
            if cursor:
                query["start_cursor"] = cursor
            endpoint = (
                f"/blocks/{urllib.parse.quote(page_id, safe='')}/children?"
                f"{urllib.parse.urlencode(query)}"
            )
            result = self.request("GET", endpoint)
            for block in result.get("results", []):
                block_type = block.get("type")
                content = block.get(block_type, {}) if block_type else {}
                rich_text = content.get("rich_text", [])
                if any(marker in item.get("plain_text", "") for item in rich_text):
                    return True
            if not result.get("has_more"):
                return False
            cursor = result.get("next_cursor")
            if not cursor:
                raise NotionSyncError(
                    "Notion indicated another page of blocks but returned no cursor."
                )

    def sync_model(
        self,
        page_id: str,
        creature_id: str,
        creature_name: str,
        glb_url: str,
        model_version: str,
        generated_at: str | None = None,
        reference_image: str | None = None,
        preview_url: str | None = None,
    ) -> bool:
        page_id = canonical_page_id(page_id)
        for candidate in (glb_url, preview_url):
            if candidate is None:
                continue
            parsed = urllib.parse.urlsplit(candidate)
            if parsed.scheme != "https" or not parsed.netloc or parsed.username:
                raise NotionSyncError("Model and preview links must use HTTPS.")

        self.request("GET", f"/pages/{urllib.parse.quote(page_id, safe='')}")
        marker = f"ruff-3d-model:{creature_id}"
        if self._has_marker(page_id, marker):
            return False

        def rich_text(content: str, url: str | None = None) -> dict[str, Any]:
            text: dict[str, Any] = {"content": content}
            if url is not None:
                text["link"] = {"url": url}
            return {"type": "text", "text": text}

        children = [
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {"rich_text": [rich_text(marker)]},
            },
            {
                "object": "block",
                "type": "heading_3",
                "heading_3": {"rich_text": [rich_text("Ruff 3D-Modell")]},
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [
                        rich_text(f"{creature_name} · {model_version} · GLB: "),
                        rich_text("Herunterladen", glb_url),
                    ]
                },
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [
                        rich_text(
                            "Status: validiert · Generiert: "
                            f"{generated_at or 'nicht dokumentiert'}"
                        )
                    ]
                },
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [
                        rich_text(
                            "Referenzbild: "
                            + (
                                reference_image.rsplit("/", maxsplit=1)[-1]
                                if reference_image
                                else "nicht dokumentiert"
                            )
                            + " · Einzelbildrekonstruktion; verdeckte Bereiche "
                            "sind nicht garantiert, Rigging wird nicht zugesichert."
                        )
                    ]
                },
            },
        ]
        if preview_url:
            children.append(
                {
                    "object": "block",
                    "type": "paragraph",
                    "paragraph": {
                        "rich_text": [
                            rich_text("Vorschau: "),
                            rich_text("Öffnen", preview_url),
                        ]
                    },
                }
            )
        response = self.request(
            "PATCH",
            f"/blocks/{urllib.parse.quote(page_id, safe='')}/children",
            {"children": children},
        )
        if not response.get("results"):
            raise NotionSyncError(
                "Notion returned success without confirming appended blocks."
            )
        return True
