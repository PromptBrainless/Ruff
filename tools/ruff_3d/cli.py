#!/usr/bin/env python3
"""Command-line entry point for Ruff's resumable Stable Fast 3D workflow."""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any

from .manifest import (
    ManifestError,
    build_manifest,
    load_manifest,
    save_manifest,
    validate_manifest,
)
from .notion import (
    NOTION_API_VERSION,
    NotionClient,
    NotionSyncError,
    model_url,
    verify_asset_url,
)
from .pipeline import (
    PipelineError,
    _relative_or_absolute,
    _resolve_path,
    _utc_now,
    generate_one,
    load_config,
    preflight,
    run_batch,
    validate_glb,
)


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CONFIG = ROOT / "tools" / "ruff_3d" / "config.json"


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser(
        "preflight",
        help="Probe runtime, GPU, weights, disk, and network.",
    )
    subparsers.add_parser("status", help="Summarize manifest completion states.")

    initialize = subparsers.add_parser(
        "init-manifest",
        help="Build or safely refresh the manifest.",
    )
    initialize.add_argument("--expected-count", type=int, default=131)

    validate = subparsers.add_parser(
        "validate-manifest",
        help="Validate manifest content and paths.",
    )
    validate.add_argument("--expected-count", type=int, default=131)

    generate = subparsers.add_parser("generate", help="Generate one creature's GLB.")
    generate.add_argument("--id", required=True, dest="creature_id")
    generate.add_argument("--allow-cpu", action="store_true")

    batch = subparsers.add_parser("batch", help="Process queued entries sequentially.")
    batch.add_argument("--limit", type=int)
    batch.add_argument("--retry-failed", action="store_true")
    batch.add_argument("--allow-cpu", action="store_true")

    sync = subparsers.add_parser(
        "sync-notion",
        help="Append validated GLB links to Notion.",
    )
    sync.add_argument("--id", dest="creature_id")

    validate_glb_command = subparsers.add_parser(
        "validate-glb", help="Validate an existing binary glTF file."
    )
    validate_glb_command.add_argument("path", type=Path)
    return parser


def _manifest_path(config: dict[str, Any]) -> Path:
    path = Path(config.get("manifest_path", "tools/ruff_3d/manifest.json"))
    return path if path.is_absolute() else ROOT / path


def _existing_entry_map(manifest: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {entry["creature_id"]: entry for entry in manifest["creatures"]}


def _merge_manifest(
    discovered: dict[str, Any], existing: dict[str, Any]
) -> dict[str, Any]:
    previous = _existing_entry_map(existing)
    merged: list[dict[str, Any]] = []
    for fresh in discovered["creatures"]:
        old = previous.get(fresh["creature_id"])
        if old is None:
            merged.append(fresh)
            continue
        entry = dict(old)
        entry.setdefault("generated_at", None)
        entry["name"] = fresh["name"]
        entry["source_record"] = fresh["source_record"]
        if fresh["reference_image"] and not entry.get("reference_image"):
            entry["reference_image"] = fresh["reference_image"]
            entry["reference_image_source"] = fresh["reference_image_source"]
            if entry["status"] == "needs_reference_image":
                entry.update(
                    status="queued",
                    generation_status="queued",
                    error=None,
                )
        elif not fresh["reference_image"]:
            if entry["status"] not in {"validated", "notion_synced"}:
                entry["reference_image"] = None
                entry["reference_image_source"] = None
                entry.update(
                    status="needs_reference_image",
                    generation_status="needs_reference_image",
                    error=(
                        "No creature-specific reference image with a verifiable "
                        "page or exact-name association was found in the local export."
                    ),
                )
        entry["updated_at"] = _utc_now()
        merged.append(entry)
    result = {"schema_version": discovered["schema_version"], "creatures": merged}
    validate_manifest(result, repository_root=ROOT)
    return result


def _get_entry(manifest: dict[str, Any], creature_id: str) -> dict[str, Any]:
    for entry in manifest["creatures"]:
        if entry["creature_id"] == creature_id:
            return entry
    raise PipelineError(f"Unknown creature ID: {creature_id}")


def _print_json(value: Any) -> None:
    print(json.dumps(value, ensure_ascii=False, indent=2))


def _run_init(config: dict[str, Any], expected_count: int) -> None:
    export_root = _resolve_path(ROOT, config.get("export_root"))
    creatures_path = _resolve_path(ROOT, config.get("creatures_path"))
    if export_root is None or creatures_path is None:
        raise ManifestError(
            "Export root and imported creature data must be configured."
        )
    manifest = build_manifest(
        creatures_path,
        export_root,
        ROOT,
        expected_count=expected_count,
    )
    destination = _manifest_path(config)
    if destination.exists():
        manifest = _merge_manifest(manifest, load_manifest(destination))
    save_manifest(destination, manifest)
    _print_json(
        {
            "manifest": _relative_or_absolute(destination, ROOT),
            "creatures": len(manifest["creatures"]),
            "reference_images": sum(
                entry["reference_image"] is not None
                for entry in manifest["creatures"]
            ),
            "needs_reference_image": sum(
                entry["status"] == "needs_reference_image"
                for entry in manifest["creatures"]
            ),
        }
    )


def _run_status(manifest: dict[str, Any]) -> None:
    statuses: dict[str, int] = {}
    generation: dict[str, int] = {}
    validation: dict[str, int] = {}
    notion: dict[str, int] = {}
    for entry in manifest["creatures"]:
        for result, key in (
            (statuses, entry["status"]),
            (generation, entry["generation_status"]),
            (validation, entry["validation_status"]),
            (notion, entry["notion_sync_status"]),
        ):
            result[key] = result.get(key, 0) + 1
    _print_json(
        {
            "creature_count": len(manifest["creatures"]),
            "with_reference_image": sum(
                entry["reference_image"] is not None
                for entry in manifest["creatures"]
            ),
            "with_model_path": sum(
                entry["model_path"] is not None
                for entry in manifest["creatures"]
            ),
            "statuses": statuses,
            "generation_statuses": generation,
            "validation_statuses": validation,
            "notion_sync_statuses": notion,
        }
    )


def _run_notion_sync(
    manifest: dict[str, Any], config: dict[str, Any], creature_id: str | None
) -> None:
    selected = [
        entry
        for entry in manifest["creatures"]
        if entry["status"] == "validated"
        and (creature_id is None or entry["creature_id"] == creature_id)
    ]
    if creature_id is not None and not selected:
        entry = _get_entry(manifest, creature_id)
        if entry["status"] != "notion_synced":
            raise PipelineError(
                "Notion synchronization is allowed only after the model validates."
            )
        _print_json({"synced": 0, "already_synced": 1})
        return
    destination = _manifest_path(config)
    token = os.environ.get("NOTION_TOKEN")
    asset_base_url = config.get("asset_base_url")
    if not token:
        for entry in selected:
            entry["notion_sync_status"] = "blocked_missing_credentials"
            entry["updated_at"] = _utc_now()
        save_manifest(destination, manifest)
        _print_json(
            {"synced": 0, "blocked": len(selected), "reason": "NOTION_TOKEN is unset"}
        )
        return
    if not asset_base_url:
        for entry in selected:
            entry["notion_sync_status"] = "blocked_no_asset_url"
            entry["updated_at"] = _utc_now()
        save_manifest(destination, manifest)
        _print_json(
            {
                "synced": 0,
                "blocked": len(selected),
                "reason": "No authorized durable HTTPS asset URL is configured",
            }
        )
        return
    if not config.get("asset_hosting_authorized", False):
        for entry in selected:
            entry["notion_sync_status"] = "blocked_asset_hosting_unapproved"
            entry["updated_at"] = _utc_now()
        save_manifest(destination, manifest)
        _print_json(
            {
                "synced": 0,
                "blocked": len(selected),
                "reason": "Asset hosting requires explicit authorization",
            }
        )
        return

    client = NotionClient(
        token,
        api_version=config.get("notion_api_version", NOTION_API_VERSION),
        max_retries=int(config.get("notion_max_retries", 3)),
    )
    counts = {"synced": 0, "already_present": 0, "failed": 0}
    for entry in selected:
        try:
            glb_url = model_url(asset_base_url, entry["creature_id"])
            verify_asset_url(glb_url)
            preview_url = entry.get("notion_preview_url")
            model_path = _resolve_path(ROOT, entry.get("model_path"))
            if model_path is None or not model_path.is_file():
                raise NotionSyncError(
                    "The validated GLB is no longer available locally."
                )
            details = validate_glb(
                model_path,
                validator_executable=config.get("gltf_validator_executable"),
                require_external=bool(
                    config.get("require_external_glb_validation", True)
                ),
            )
            if details["sha256"] != entry.get("model_sha256"):
                raise NotionSyncError(
                    "The GLB changed after validation; validate again."
                )
            entry["notion_model_url"] = glb_url
            changed = client.sync_model(
                page_id=entry["notion_page_id"],
                creature_id=entry["creature_id"],
                creature_name=entry["name"],
                glb_url=glb_url,
                model_version=entry.get("model_version") or "Stable Fast 3D",
                generated_at=entry.get("generated_at"),
                reference_image=entry.get("reference_image"),
                preview_url=preview_url,
            )
            entry["status"] = "notion_synced"
            entry["notion_sync_status"] = "synced"
            entry["error"] = None
            counts["synced" if changed else "already_present"] += 1
        except NotionSyncError as error:
            entry["notion_sync_status"] = "failed"
            entry["error"] = str(error)
            counts["failed"] += 1
        entry["updated_at"] = _utc_now()
        save_manifest(destination, manifest)
    _print_json(counts)


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    try:
        config = load_config(args.config, ROOT)
        if args.command == "init-manifest":
            _run_init(config, args.expected_count)
            return 0
        if args.command == "validate-glb":
            _print_json(
                validate_glb(
                    args.path.resolve(),
                    validator_executable=config.get("gltf_validator_executable"),
                    require_external=bool(
                        config.get("require_external_glb_validation", True)
                    ),
                )
            )
            return 0
        if args.command == "preflight":
            _print_json(preflight(config, ROOT))
            return 0

        manifest_path = _manifest_path(config)
        manifest = load_manifest(manifest_path)
        if args.command == "validate-manifest":
            validate_manifest(
                manifest,
                repository_root=ROOT,
                expected_count=args.expected_count,
            )
            print(f"Manifest validation passed: {len(manifest['creatures'])} entries.")
        elif args.command == "status":
            _run_status(manifest)
        elif args.command == "generate":
            entry = _get_entry(manifest, args.creature_id)
            status = generate_one(
                manifest,
                entry,
                {**config, "manifest_path": str(manifest_path)},
                repository_root=ROOT,
                allow_cpu=args.allow_cpu,
            )
            print(f"{entry['creature_id']} {entry['name']}: {status}")
        elif args.command == "batch":
            counts = run_batch(
                manifest,
                config,
                repository_root=ROOT,
                limit=args.limit,
                retry_failed=args.retry_failed,
                allow_cpu=args.allow_cpu,
            )
            _print_json({"processed": counts})
        elif args.command == "sync-notion":
            _run_notion_sync(manifest, config, args.creature_id)
        else:
            raise PipelineError(f"Unsupported command: {args.command}")
    except (
        ManifestError,
        PipelineError,
        NotionSyncError,
        OSError,
        ValueError,
    ) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
