#!/usr/bin/env python3
"""Build and validate the stable-ID manifest for Ruff creature models."""

from __future__ import annotations

import json
import os
import re
import tempfile
import unicodedata
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Any


MANIFEST_VERSION = 1
NOTION_ID_PATTERN = re.compile(r"([0-9a-f]{32})\.html$", re.IGNORECASE)
IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp"}


class ManifestError(ValueError):
    """Raised when an input dataset cannot produce a trustworthy manifest."""


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def normalize_name(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value).casefold()
    return "".join(character for character in normalized if character.isalnum())


class _ImageParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.sources: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag == "img":
            self.sources.extend(value for key, value in attrs if key == "src" and value)


def _local_page_images(source_page: Path, export_root: Path) -> list[Path]:
    parser = _ImageParser()
    parser.feed(source_page.read_text(encoding="utf-8", errors="replace"))
    candidates: list[Path] = []
    root = export_root.resolve()
    for source in parser.sources:
        if source.startswith(("http://", "https://", "data:", "file:")):
            continue
        image_path = (source_page.parent / source.split("?", maxsplit=1)[0]).resolve()
        if (
            image_path.suffix.lower() in IMAGE_SUFFIXES
            and image_path.is_file()
            and image_path.is_relative_to(root)
        ):
            candidates.append(image_path)
    return candidates


def _find_reference_image(
    name: str,
    source_page: Path,
    export_root: Path,
    image_index: dict[str, list[Path]],
) -> tuple[Path | None, str | None]:
    page_images = _local_page_images(source_page, export_root)
    exact_page_matches = [
        path
        for path in page_images
        if normalize_name(path.stem) == normalize_name(name)
    ]
    if len(exact_page_matches) == 1:
        return exact_page_matches[0], "embedded on the creature's own exported page"
    if len(exact_page_matches) > 1:
        return None, None

    exact_name_matches = image_index.get(normalize_name(name), [])
    if len(exact_name_matches) == 1:
        return (
            exact_name_matches[0],
            "unique export image filename exactly matches the creature name",
        )
    return None, None


def build_manifest(
    creatures_path: Path,
    export_root: Path,
    repository_root: Path,
    expected_count: int | None = 131,
) -> dict[str, Any]:
    creatures = json.loads(creatures_path.read_text(encoding="utf-8"))
    if not isinstance(creatures, list):
        raise ManifestError("The creature data must be a JSON array.")
    if expected_count is not None and len(creatures) != expected_count:
        raise ManifestError(
            f"Expected {expected_count} imported creatures; found {len(creatures)}."
        )

    export_root = export_root.resolve()
    repository_root = repository_root.resolve()
    image_index: dict[str, list[Path]] = {}
    for path in sorted(export_root.rglob("*")):
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES:
            image_index.setdefault(normalize_name(path.stem), []).append(path.resolve())

    entries: list[dict[str, Any]] = []
    seen_ids: set[str] = set()
    for creature in creatures:
        name = creature.get("name")
        source = creature.get("source")
        if not isinstance(name, str) or not name.strip():
            raise ManifestError("Every creature needs a non-empty name.")
        if not isinstance(source, str) or not source:
            raise ManifestError(f"{name}: no source page was recorded.")

        source_page = (export_root / source).resolve()
        if not source_page.is_file() or not source_page.is_relative_to(export_root):
            raise ManifestError(
                f"{name}: source page is missing or outside the export."
            )
        match = NOTION_ID_PATTERN.search(source_page.name)
        if match is None:
            raise ManifestError(
                f"{name}: source filename has no exported Notion page ID."
            )
        notion_page_id = match.group(1).lower()
        if notion_page_id in seen_ids:
            raise ManifestError(f"Duplicate exported page ID: {notion_page_id}.")
        seen_ids.add(notion_page_id)

        image_path, image_reason = _find_reference_image(
            name, source_page, export_root, image_index
        )
        reference_image: str | None = None
        reference_image_source: str | None = None
        if image_path is not None:
            if not image_path.is_relative_to(repository_root):
                raise ManifestError(
                    f"{name}: reference image is outside the repository."
                )
            reference_image = image_path.relative_to(repository_root).as_posix()
            reference_image_source = (
                f"{reference_image} ({image_reason}; preserved Notion export asset)"
            )

        needs_image = reference_image is None
        entries.append(
            {
                "creature_id": notion_page_id,
                "name": name,
                "source_record": source_page.relative_to(repository_root).as_posix(),
                "reference_image": reference_image,
                "reference_image_source": reference_image_source,
                "status": "needs_reference_image" if needs_image else "queued",
                "generation_status": (
                    "needs_reference_image" if needs_image else "queued"
                ),
                "model_path": None,
                "model_format": None,
                "model_size_bytes": None,
                "model_sha256": None,
                "model_version": None,
                "generated_at": None,
                "validation_status": "not_started",
                "validation_details": None,
                "notion_page_id": notion_page_id,
                "notion_sync_status": "not_attempted",
                "notion_model_url": None,
                "notion_preview_url": None,
                "error": (
                    "No creature-specific reference image with a verifiable "
                    "page or exact-name association was found in the local export."
                    if needs_image
                    else None
                ),
                "attempt_count": 0,
                "updated_at": utc_now(),
            }
        )

    manifest = {"schema_version": MANIFEST_VERSION, "creatures": entries}
    validate_manifest(manifest, repository_root=repository_root)
    return manifest


def validate_manifest(
    manifest: Any,
    repository_root: Path | None = None,
    expected_count: int | None = None,
) -> None:
    if (
        not isinstance(manifest, dict)
        or manifest.get("schema_version") != MANIFEST_VERSION
    ):
        raise ManifestError("Manifest must be an object with schema_version 1.")
    creatures = manifest.get("creatures")
    if not isinstance(creatures, list):
        raise ManifestError("Manifest 'creatures' must be an array.")
    if expected_count is not None and len(creatures) != expected_count:
        raise ManifestError(
            f"Expected {expected_count} manifest entries; found {len(creatures)}."
        )

    seen_ids: set[str] = set()
    seen_pages: set[str] = set()
    for entry in creatures:
        if not isinstance(entry, dict):
            raise ManifestError("Every manifest entry must be an object.")
        creature_id = entry.get("creature_id")
        if not isinstance(creature_id, str) or not re.fullmatch(
            r"[0-9a-f]{32}", creature_id
        ):
            raise ManifestError("creature_id must be an exported 32-character page ID.")
        if creature_id in seen_ids:
            raise ManifestError(f"Duplicate creature_id: {creature_id}.")
        seen_ids.add(creature_id)

        page_id = entry.get("notion_page_id")
        if page_id != creature_id:
            raise ManifestError(
                f"{creature_id}: notion_page_id must match the source export ID."
            )
        if page_id in seen_pages:
            raise ManifestError(f"Duplicate notion_page_id: {page_id}.")
        seen_pages.add(page_id)
        if not isinstance(entry.get("name"), str) or not entry["name"].strip():
            raise ManifestError(f"{creature_id}: name must not be empty.")
        if entry.get("status") not in {
            "queued",
            "needs_reference_image",
            "blocked_no_gpu",
            "generating",
            "generated",
            "validating",
            "validated",
            "notion_synced",
            "failed",
        }:
            raise ManifestError(f"{creature_id}: invalid overall status.")
        if entry.get("generation_status") not in {
            "queued",
            "needs_reference_image",
            "blocked_no_gpu",
            "generating",
            "generated",
            "failed",
        }:
            raise ManifestError(f"{creature_id}: invalid generation_status.")
        if entry.get("validation_status") not in {
            "not_started",
            "validating",
            "validated",
            "failed",
        }:
            raise ManifestError(f"{creature_id}: invalid validation_status.")
        if (
            not isinstance(entry.get("attempt_count"), int)
            or entry["attempt_count"] < 0
        ):
            raise ManifestError(f"{creature_id}: attempt_count must be non-negative.")
        if entry.get("reference_image") is None:
            if entry.get("status") != "needs_reference_image":
                raise ManifestError(
                    f"{creature_id}: missing image must be explicitly marked."
                )
            if not entry.get("error"):
                raise ManifestError(
                    f"{creature_id}: missing image must have an explanatory error."
                )
        elif not isinstance(entry.get("reference_image"), str):
            raise ManifestError(
                f"{creature_id}: reference_image must be a path or null."
            )
        elif repository_root is not None:
            image_path = (repository_root / entry["reference_image"]).resolve()
            if (
                not image_path.is_relative_to(repository_root.resolve())
                or not image_path.is_file()
            ):
                raise ManifestError(
                    f"{creature_id}: reference image path is missing or unsafe."
                )
        model_path = entry.get("model_path")
        if model_path is not None and not isinstance(model_path, str):
            raise ManifestError(f"{creature_id}: model_path must be a path or null.")
        if entry.get("model_format") not in {None, "glb"}:
            raise ManifestError(f"{creature_id}: model_format must be glb or null.")
        generated_at = entry.get("generated_at")
        if generated_at is not None and not isinstance(generated_at, str):
            raise ManifestError(
                f"{creature_id}: generated_at must be a timestamp or null."
            )
        size = entry.get("model_size_bytes")
        if size is not None and (not isinstance(size, int) or size <= 0):
            raise ManifestError(
                f"{creature_id}: model_size_bytes must be positive or null."
            )
        if entry.get("status") in {"validated", "notion_synced"}:
            if (
                not isinstance(model_path, str)
                or entry.get("model_format") != "glb"
                or entry.get("validation_status") != "validated"
                or not isinstance(size, int)
                or not entry.get("model_sha256")
            ):
                raise ManifestError(
                    f"{creature_id}: completed models need a path, size, hash, "
                    "and validation."
                )
            if repository_root is not None:
                model_file = Path(model_path).expanduser()
                if not model_file.is_absolute():
                    model_file = repository_root / model_file
                if not model_file.resolve().is_file():
                    raise ManifestError(
                        f"{creature_id}: validated model file is missing."
                    )


def load_manifest(path: Path) -> dict[str, Any]:
    try:
        manifest = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ManifestError(f"Cannot read manifest {path}: {error}") from error
    validate_manifest(manifest)
    return manifest


def save_manifest(path: Path, manifest: dict[str, Any]) -> None:
    validate_manifest(manifest)
    path.parent.mkdir(parents=True, exist_ok=True)
    content = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=f".{path.name}.", suffix=".tmp", dir=path.parent
    )
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as file:
            file.write(content)
            file.flush()
            os.fsync(file.fileno())
        os.replace(temporary_name, path)
    except OSError:
        try:
            os.unlink(temporary_name)
        except FileNotFoundError:
            pass
        raise
