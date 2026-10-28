#!/usr/bin/env python3
"""Check Markdown links to local files and HTTP(S) pages."""

import re
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import unquote, urlparse
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
LINK_PATTERN = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
IGNORED_PARTS = {".git", ".gradle", ".source-export", "build", "ref"}


def extract_links(markdown: str) -> list[str]:
    links = []
    in_fence = False
    for line in markdown.splitlines():
        if line.lstrip().startswith("```"):
            in_fence = not in_fence
            continue
        if not in_fence:
            links.extend(
                match.group(1).strip() for match in LINK_PATTERN.finditer(line)
            )
    return links


def check_external_link(url: str) -> str | None:
    request = Request(url, headers={"User-Agent": "Ruff-doc-link-check/1.0"})
    try:
        with urlopen(request, timeout=15) as response:
            if response.status >= 400:
                return f"HTTP {response.status}"
    except HTTPError as error:
        return f"HTTP {error.code}"
    except (URLError, TimeoutError) as error:
        return str(error)
    return None


def check_docs(root: Path, check_external: bool = True) -> list[str]:
    errors = []
    for markdown_path in sorted(root.rglob("*.md")):
        if IGNORED_PARTS.intersection(markdown_path.relative_to(root).parts):
            continue
        try:
            markdown = markdown_path.read_text(encoding="utf-8")
        except OSError as error:
            errors.append(f"{markdown_path}: cannot read Markdown: {error}")
            continue

        for destination in extract_links(markdown):
            destination = destination.split(maxsplit=1)[0].strip("<>")
            parsed = urlparse(destination)
            if parsed.scheme in {"http", "https"}:
                if check_external:
                    error = check_external_link(destination)
                    if error:
                        errors.append(f"{markdown_path}: {destination}: {error}")
                continue
            if parsed.scheme or not parsed.path:
                continue
            target = (markdown_path.parent / unquote(parsed.path)).resolve()
            if not target.exists():
                errors.append(f"{markdown_path}: missing local target {destination}")
    return errors


def main() -> int:
    errors = check_docs(ROOT)
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        print(
            f"Documentation link check failed with {len(errors)} error(s).",
            file=sys.stderr,
        )
        return 1
    print("Documentation links passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
