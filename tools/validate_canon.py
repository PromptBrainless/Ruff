#!/usr/bin/env python3
"""Validate Ruff bestiary records and their cross-references."""

import argparse
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATA_DIR = ROOT / "bestiarium" / "daten"
REQUIRED_FIELDS = {
    "id",
    "names",
    "origin",
    "behavior",
    "appearance",
    "special_features",
    "core_values",
    "power_level_source",
    "status",
    "source",
}
TEXT_FIELDS = {
    "origin",
    "behavior",
    "appearance",
    "special_features",
    "source",
}
VALID_STATUSES = {"bestätigt", "Vorschlag"}


def validate_records(data_dir: Path) -> list[str]:
    errors = []
    records = []
    for path in sorted(data_dir.rglob("*.json")):
        try:
            record = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            errors.append(f"{path}: cannot read JSON: {error}")
            continue
        if not isinstance(record, dict):
            errors.append(f"{path}: each file must contain one JSON object")
            continue
        records.append((path, record))

    ids = {}
    names = {}
    for path, record in records:
        missing = sorted(REQUIRED_FIELDS - record.keys())
        if missing:
            errors.append(f"{path}: missing required fields: {', '.join(missing)}")

        entry_id = record.get("id")
        if isinstance(entry_id, str) and entry_id:
            if entry_id in ids:
                errors.append(
                    f"{path}: duplicate id {entry_id!r} (also in {ids[entry_id]})"
                )
            else:
                ids[entry_id] = path
        else:
            errors.append(f"{path}: id must be a non-empty string")

        entry_names = record.get("names")
        if not isinstance(entry_names, list) or not entry_names:
            errors.append(f"{path}: names must be a non-empty list")
        else:
            for name in entry_names:
                if not isinstance(name, str) or not name.strip():
                    errors.append(f"{path}: every name must be a non-empty string")
                    continue
                normalized = name.strip().casefold()
                if normalized in names:
                    errors.append(
                        f"{path}: duplicate name {name!r} (also in {names[normalized]})"
                    )
                else:
                    names[normalized] = path

        for field in TEXT_FIELDS:
            value = record.get(field)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"{path}: {field} must be a non-empty string")

        core_values = record.get("core_values")
        if not isinstance(core_values, dict) or not core_values:
            errors.append(f"{path}: core_values must be a non-empty object")
        else:
            for attribute, value in core_values.items():
                if type(value) is not int or not 0 <= value <= 1000:
                    errors.append(
                        f"{path}: core_values.{attribute} must be an integer "
                        "from 0 to 1000"
                    )

        level_source = record.get("power_level_source")
        if not isinstance(level_source, dict) or any(
            not isinstance(level_source.get(field), str)
            or not level_source[field].strip()
            for field in ("value", "source")
        ):
            errors.append(
                f"{path}: power_level_source needs non-empty value and source "
                "strings"
            )

        status = record.get("status")
        if not isinstance(status, str) or status not in VALID_STATUSES:
            errors.append(
                f"{path}: status must be 'bestätigt' or 'Vorschlag'"
            )

        related_entries = record.get("related_entries", [])
        if not isinstance(related_entries, list) or any(
            not isinstance(reference, str) for reference in related_entries
        ):
            errors.append(f"{path}: related_entries must be a list of entry IDs")

    for path, record in records:
        references = record.get("related_entries", [])
        if not isinstance(references, list):
            continue
        for reference in references:
            if isinstance(reference, str) and reference not in ids:
                errors.append(
                    f"{path}: orphan related_entries reference {reference!r}"
                )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--data-dir",
        type=Path,
        default=DEFAULT_DATA_DIR,
        help="Directory containing one JSON object per bestiary entry",
    )
    args = parser.parse_args()

    if not args.data_dir.is_dir():
        print(f"Bestiary data directory not found: {args.data_dir}", file=sys.stderr)
        return 2

    errors = validate_records(args.data_dir)
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        print(
            f"Canon validation failed with {len(errors)} error(s).",
            file=sys.stderr,
        )
        return 1

    count = sum(1 for _ in args.data_dir.rglob("*.json"))
    suffix = "y" if count == 1 else "ies"
    print(f"Canon validation passed: {count} bestiary entr{suffix}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
