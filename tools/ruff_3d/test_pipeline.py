from __future__ import annotations

import json
import io
import struct
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from .manifest import ManifestError, build_manifest, validate_manifest
from .notion import (
    NOTION_API_VERSION,
    NotionClient,
    NotionSyncError,
    model_url,
)
from .pipeline import (
    SF3D_COMMIT,
    SF3D_WEIGHTS_REVISION,
    PipelineError,
    _weights_snapshot,
    generate_one,
    run_batch,
    validate_glb,
)


def create_glb(path: Path) -> None:
    binary = struct.pack("<9f", 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    document = {
        "asset": {"version": "2.0"},
        "buffers": [{"byteLength": len(binary)}],
        "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": len(binary)}],
        "accessors": [
            {
                "bufferView": 0,
                "componentType": 5126,
                "count": 3,
                "type": "VEC3",
                "min": [0.0, 0.0, 0.0],
                "max": [1.0, 1.0, 0.0],
            }
        ],
        "materials": [{"name": "Ruff material"}],
        "meshes": [
            {
                "primitives": [
                    {"attributes": {"POSITION": 0}, "material": 0}
                ]
            }
        ],
        "nodes": [{"mesh": 0}],
        "scenes": [{"nodes": [0]}],
        "scene": 0,
    }
    json_chunk = json.dumps(document, separators=(",", ":")).encode("utf-8")
    json_chunk += b" " * (-len(json_chunk) % 4)
    binary += b"\x00" * (-len(binary) % 4)
    chunks = (
        struct.pack("<I4s", len(json_chunk), b"JSON")
        + json_chunk
        + struct.pack("<I4s", len(binary), b"BIN\x00")
        + binary
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(struct.pack("<4sII", b"glTF", 2, 12 + len(chunks)) + chunks)


def manifest_entry(
    creature_id: str = "0123456789abcdef0123456789abcdef",
    image: str | None = "reference.png",
) -> dict:
    missing = image is None
    return {
        "creature_id": creature_id,
        "name": "Testkreatur",
        "source_record": ".source-export/creature.html",
        "reference_image": image,
        "reference_image_source": "test fixture" if image else None,
        "status": "needs_reference_image" if missing else "queued",
        "generation_status": "needs_reference_image" if missing else "queued",
        "model_path": None,
        "model_format": None,
        "model_size_bytes": None,
        "model_sha256": None,
        "model_version": None,
        "generated_at": None,
        "validation_status": "not_started",
        "validation_details": None,
        "notion_page_id": creature_id,
        "notion_sync_status": "not_attempted",
        "notion_model_url": None,
        "notion_preview_url": None,
        "error": "No verified image." if missing else None,
        "attempt_count": 0,
        "updated_at": "2026-10-28T08:00:00+00:00",
    }


def manifest_for(entry: dict | None = None) -> dict:
    return {"schema_version": 1, "creatures": [entry or manifest_entry()]}


def generation_config(root: Path, **overrides) -> dict:
    config = {
        "sf3d_repo": "sf3d",
        "sf3d_commit": SF3D_COMMIT,
        "license_reviewed": True,
        "python_executable": "python",
        "pretrained_model": "stabilityai/stable-fast-3d",
        "manifest_path": str(root / "manifest.json"),
        "output_dir": ".artifacts/models",
        "log_dir": ".artifacts/logs",
        "timeout_seconds": 10,
        "max_attempts": 2,
        "retry_delay_seconds": 0,
        "parallelism": 1,
        "texture_resolution": 1024,
        "remesh_option": "none",
        "target_vertex_count": -1,
        "gltf_validator_executable": "gltf_validator",
        "require_external_glb_validation": False,
    }
    config.update(overrides)
    return config


class ManifestTests(unittest.TestCase):
    def test_repository_manifest_has_131_unique_ids_and_five_verified_images(self):
        root = Path(__file__).resolve().parents[2]
        path = root / "tools" / "ruff_3d" / "manifest.json"
        manifest = json.loads(path.read_text(encoding="utf-8"))
        validate_manifest(manifest, expected_count=131)
        entries = manifest["creatures"]
        self.assertEqual(len({entry["creature_id"] for entry in entries}), 131)
        self.assertEqual(len({entry["notion_page_id"] for entry in entries}), 131)
        self.assertEqual(sum(bool(entry["reference_image"]) for entry in entries), 5)
        self.assertEqual(
            sum(entry["status"] == "needs_reference_image" for entry in entries),
            126,
        )

    def test_manifest_build_uses_export_ids_and_exact_filename_matches(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            export = root / ".source-export"
            pages = export / "Wesen & Figuren" / "Wesen-Register"
            page_id = "a" * 32
            page = pages / f"Werwolf {page_id}.html"
            page.parent.mkdir(parents=True)
            page.write_text("<h1>Werwolf</h1>", encoding="utf-8")
            images = export / "Wesen & Figuren" / "Machtstufe III"
            images.mkdir(parents=True)
            (images / "werwolf.png").write_bytes(b"test")
            (images / "werwolf-aehnlich.png").write_bytes(b"test")
            creatures = root / "creatures.json"
            creatures.write_text(
                json.dumps(
                    [
                        {
                            "name": "Werwolf",
                            "source": page.relative_to(export).as_posix(),
                        }
                    ]
                ),
                encoding="utf-8",
            )

            result = build_manifest(
                creatures,
                export,
                root,
                expected_count=1,
            )

        entry = result["creatures"][0]
        self.assertEqual(entry["creature_id"], page_id)
        self.assertEqual(entry["notion_page_id"], page_id)
        expected_image = images.relative_to(root) / "werwolf.png"
        self.assertEqual(entry["reference_image"], expected_image.as_posix())
        self.assertEqual(entry["status"], "queued")

    def test_manifest_rejects_duplicate_ids_and_wrong_target_count(self):
        duplicate = manifest_for()
        duplicate["creatures"].append(manifest_entry())
        with self.assertRaisesRegex(ManifestError, "Duplicate creature_id"):
            validate_manifest(duplicate)
        with self.assertRaisesRegex(ManifestError, "Expected 131"):
            validate_manifest(manifest_for(), expected_count=131)

    def test_manifest_rejects_missing_local_reference_image(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            with self.assertRaisesRegex(
                ManifestError,
                "reference image path is missing or unsafe",
            ):
                validate_manifest(
                    manifest_for(),
                    repository_root=Path(temporary_directory),
                )

    def test_weights_lookup_requires_the_pinned_snapshot(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            cache = (
                Path(temporary_directory)
                / "models--stabilityai--stable-fast-3d"
                / "snapshots"
                / SF3D_WEIGHTS_REVISION
            )
            cache.mkdir(parents=True)
            (cache / "config.yaml").write_text("model: test", encoding="utf-8")
            (cache / "model.safetensors").write_bytes(b"weights")
            with patch.dict(
                "os.environ",
                {"HF_HUB_CACHE": str(Path(temporary_directory))},
                clear=True,
            ):
                snapshot = _weights_snapshot(
                    "stabilityai/stable-fast-3d",
                    SF3D_WEIGHTS_REVISION,
                )
                wrong_revision = _weights_snapshot(
                    "stabilityai/stable-fast-3d",
                    "a" * 40,
                )
        self.assertEqual(snapshot, cache.resolve())
        self.assertIsNone(wrong_revision)


class GlbValidationTests(unittest.TestCase):
    def test_valid_glb_reports_real_mesh_and_material_counts(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            path = Path(temporary_directory) / "model.glb"
            create_glb(path)
            result = validate_glb(path)

        self.assertGreater(result["file_size_bytes"], 0)
        self.assertEqual(result["glb_version"], 2)
        self.assertEqual(result["mesh_count"], 1)
        self.assertEqual(result["primitive_count"], 1)
        self.assertEqual(result["material_count"], 1)
        self.assertEqual(len(result["sha256"]), 64)

    def test_glb_rejects_empty_or_incorrect_header(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            path = Path(temporary_directory) / "empty.glb"
            path.write_bytes(b"")
            with self.assertRaisesRegex(PipelineError, "does not exist|empty"):
                validate_glb(path)
            path.write_bytes(b"not-a-glb-file-which-is-long-enough")
            with self.assertRaisesRegex(PipelineError, "magic"):
                validate_glb(path)

    def test_production_validation_requires_khronos_validator(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            path = Path(temporary_directory) / "model.glb"
            create_glb(path)
            with self.assertRaisesRegex(PipelineError, "Khronos gltf_validator"):
                validate_glb(path, "missing-gltf-validator", require_external=True)


class GenerationTests(unittest.TestCase):
    def setUp(self):
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)
        (self.root / "sf3d" / "run.py").parent.mkdir(parents=True)
        (self.root / "sf3d" / "run.py").write_text(
            "from __future__ import annotations\n", encoding="utf-8"
        )
        self.image = self.root / "reference.png"
        self.image.write_bytes(b"test image")
        self.weights = self.root / "weights" / SF3D_WEIGHTS_REVISION
        self.weights.mkdir(parents=True)
        (self.weights / "config.yaml").write_text("model: test", encoding="utf-8")
        (self.weights / "model.safetensors").write_bytes(b"test weights")
        self.entry = manifest_entry()
        self.manifest = manifest_for(self.entry)
        self.config = generation_config(
            self.root,
            pretrained_model=str(self.weights),
        )

    def tearDown(self):
        self.temporary_directory.cleanup()

    def _successful_run(self, command, **_kwargs):
        if command[0] == "git":
            return subprocess.CompletedProcess(command, 0, SF3D_COMMIT + "\n", "")
        output_dir = Path(command[command.index("--output-dir") + 1])
        create_glb(output_dir / "0" / "mesh.glb")
        return subprocess.CompletedProcess(command, 0, "generated", "")

    def test_no_gpu_blocks_without_attempt_or_subprocess(self):
        with patch("tools.ruff_3d.pipeline.subprocess.run") as run:
            status = generate_one(
                self.manifest,
                self.entry,
                self.config,
                self.root,
                gpu_available=False,
            )

        self.assertEqual(status, "blocked_no_gpu")
        self.assertEqual(self.entry["attempt_count"], 0)
        run.assert_not_called()

    def test_inference_environment_excludes_non_model_credentials(self):
        from .pipeline import _sf3d_environment

        with patch.dict(
            "os.environ",
            {
                "HF_TOKEN": "hf-test",
            },
            clear=True,
        ):
            environment = _sf3d_environment()
        self.assertNotIn("HF_TOKEN", environment)

    def test_missing_pinned_weights_blocks_generation_without_inference(self):
        self.config["pretrained_model"] = "stabilityai/stable-fast-3d"
        with patch.dict(
            "os.environ",
            {"HF_HUB_CACHE": str(self.root / "empty-cache")},
            clear=True,
        ):
            with patch(
                "tools.ruff_3d.pipeline.subprocess.run",
                return_value=subprocess.CompletedProcess(
                    ["git"], 0, SF3D_COMMIT + "\n", ""
                ),
            ) as run:
                status = generate_one(
                    self.manifest,
                    self.entry,
                    self.config,
                    self.root,
                    gpu_available=True,
                )
        self.assertEqual(status, "failed")
        self.assertIn(
            "pinned Stable Fast 3D weights are unavailable",
            self.entry["error"],
        )
        self.assertEqual(self.entry["attempt_count"], 0)
        run.assert_called_once()

    def test_missing_image_is_not_generated(self):
        entry = manifest_entry(image=None)
        manifest = manifest_for(entry)
        with patch("tools.ruff_3d.pipeline.subprocess.run") as run:
            status = generate_one(
                manifest,
                entry,
                self.config,
                self.root,
                gpu_available=True,
            )
        self.assertEqual(status, "needs_reference_image")
        self.assertEqual(entry["attempt_count"], 0)
        run.assert_not_called()

    def test_reuses_existing_validated_model_without_regeneration(self):
        model = (
            self.root
            / ".artifacts"
            / "models"
            / self.entry["creature_id"]
            / "model.glb"
        )
        create_glb(model)
        details = validate_glb(model)
        self.entry.update(
            status="validated",
            generation_status="generated",
            model_path=model.relative_to(self.root).as_posix(),
            model_format="glb",
            model_size_bytes=details["file_size_bytes"],
            model_sha256=details["sha256"],
            validation_status="validated",
        )
        with patch("tools.ruff_3d.pipeline.subprocess.run") as run:
            status = generate_one(
                self.manifest, self.entry, self.config, self.root, gpu_available=True
            )
        self.assertEqual(status, "validated")
        self.assertEqual(self.entry["validation_status"], "validated")
        self.assertEqual(self.entry["model_size_bytes"], model.stat().st_size)
        run.assert_not_called()

    def test_reusing_model_preserves_notion_synced_state(self):
        model = (
            self.root
            / ".artifacts"
            / "models"
            / self.entry["creature_id"]
            / "model.glb"
        )
        create_glb(model)
        details = validate_glb(model)
        self.entry.update(
            status="notion_synced",
            generation_status="generated",
            notion_sync_status="synced",
            model_path=model.relative_to(self.root).as_posix(),
            model_format="glb",
            model_size_bytes=details["file_size_bytes"],
            model_sha256=details["sha256"],
            validation_status="validated",
        )
        with patch("tools.ruff_3d.pipeline.subprocess.run") as run:
            status = generate_one(
                self.manifest, self.entry, self.config, self.root, gpu_available=True
            )
        self.assertEqual(status, "notion_synced")
        self.assertEqual(self.entry["notion_sync_status"], "synced")
        run.assert_not_called()

    def test_untracked_glb_is_not_treated_as_a_successful_prior_run(self):
        model = (
            self.root
            / ".artifacts"
            / "models"
            / self.entry["creature_id"]
            / "model.glb"
        )
        create_glb(model)
        with patch(
            "tools.ruff_3d.pipeline.subprocess.run",
            side_effect=self._successful_run,
        ) as run:
            status = generate_one(
                self.manifest,
                self.entry,
                self.config,
                self.root,
                gpu_available=True,
            )
        self.assertEqual(status, "validated")
        self.assertEqual(run.call_count, 2)

    def test_retries_once_then_records_valid_glb(self):
        calls = 0

        def sometimes_fails(command, **kwargs):
            nonlocal calls
            if command[0] == "git":
                return subprocess.CompletedProcess(command, 0, SF3D_COMMIT + "\n", "")
            calls += 1
            if calls == 1:
                return subprocess.CompletedProcess(command, 2, "", "transient")
            return self._successful_run(command)

        with patch(
            "tools.ruff_3d.pipeline.subprocess.run",
            side_effect=sometimes_fails,
        ):
            status = generate_one(
                self.manifest,
                self.entry,
                self.config,
                self.root,
                gpu_available=True,
            )

        self.assertEqual(status, "validated")
        self.assertEqual(self.entry["attempt_count"], 2)
        self.assertTrue((self.root / self.entry["model_path"]).is_file())
        self.assertEqual(self.entry["model_format"], "glb")
        self.assertEqual(self.entry["validation_status"], "validated")

    def test_failure_is_bounded_and_persisted(self):
        with patch(
            "tools.ruff_3d.pipeline.subprocess.run",
            side_effect=[
                subprocess.CompletedProcess(["git"], 0, SF3D_COMMIT + "\n", ""),
                subprocess.CompletedProcess(["sf3d"], 2, "", "first failure"),
                subprocess.CompletedProcess(["sf3d"], 2, "", "second failure"),
            ],
        ):
            status = generate_one(
                self.manifest,
                self.entry,
                self.config,
                self.root,
                gpu_available=True,
            )
        self.assertEqual(status, "failed")
        self.assertEqual(self.entry["attempt_count"], 2)
        self.assertIn("status 2", self.entry["error"])
        self.assertEqual(self.entry["validation_status"], "not_started")

    def test_unreviewed_license_blocks_inference(self):
        self.config["license_reviewed"] = False
        with patch("tools.ruff_3d.pipeline.subprocess.run") as run:
            status = generate_one(
                self.manifest,
                self.entry,
                self.config,
                self.root,
                gpu_available=True,
            )
        self.assertEqual(status, "failed")
        self.assertEqual(self.entry["attempt_count"], 0)
        self.assertIn("license", self.entry["error"])
        run.assert_not_called()

    def test_batch_recovers_interrupted_generation_state(self):
        self.entry.update(
            status="generating",
            generation_status="generating",
            attempt_count=1,
        )
        self.config["manifest_path"] = "manifest.json"
        with patch(
            "tools.ruff_3d.pipeline._gpu_report",
            return_value={"available": False},
        ):
            counts = run_batch(self.manifest, self.config, self.root)
        self.assertEqual(counts, {"blocked_no_gpu": 1})
        self.assertEqual(self.entry["status"], "blocked_no_gpu")
        self.assertIn("no CUDA/MPS GPU", self.entry["error"])


class _FakeResponse:
    def __init__(self, value: dict):
        self.value = value
        self.status = 200

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self):
        return json.dumps(self.value).encode("utf-8")

    def geturl(self):
        return "https://assets.example/model.glb"


class NotionTests(unittest.TestCase):
    def test_page_id_and_asset_url_use_stable_id(self):
        page_id = "abcdef0123456789abcdef0123456789"
        self.assertEqual(
            model_url("https://assets.example/models", page_id),
            f"https://assets.example/models/{page_id}/model.glb",
        )
        self.assertEqual(NOTION_API_VERSION, "2022-06-28")
        with self.assertRaisesRegex(NotionSyncError, "HTTPS"):
            model_url("http://assets.example/models", page_id)

    def test_missing_secret_fails_explicitly_without_network(self):
        with self.assertRaisesRegex(NotionSyncError, "NOTION_TOKEN"):
            NotionClient("")

    def test_rate_limit_retries_using_retry_after(self):
        calls = 0
        sleeps: list[float] = []

        def opener(_request, timeout):
            nonlocal calls
            self.assertEqual(timeout, 20)
            calls += 1
            if calls == 1:
                raise __import__("urllib.error").error.HTTPError(
                    "https://api.notion.com",
                    429,
                    "rate limited",
                    {"Retry-After": "0"},
                    io.BytesIO(b""),
                )
            return _FakeResponse({"ok": True})

        client = NotionClient("test-token", opener=opener, sleep=sleeps.append)
        self.assertEqual(client.request("GET", "/pages/id"), {"ok": True})
        self.assertEqual(calls, 2)
        self.assertEqual(sleeps, [0.0])

    def test_gateway_overload_retries_and_respects_retry_after(self):
        calls = 0
        sleeps: list[float] = []

        def opener(_request, timeout):
            nonlocal calls
            calls += 1
            if calls == 1:
                raise __import__("urllib.error").error.HTTPError(
                    "https://api.notion.com",
                    529,
                    "overloaded",
                    {"Retry-After": "75"},
                    io.BytesIO(b""),
                )
            return _FakeResponse({"ok": True})

        client = NotionClient("test-token", opener=opener, sleep=sleeps.append)
        self.assertEqual(client.request("GET", "/pages/id"), {"ok": True})
        self.assertEqual(sleeps, [75.0])

    def test_public_api_blocked_rate_limit_is_not_retried(self):
        calls = 0

        def opener(_request, timeout):
            nonlocal calls
            calls += 1
            raise __import__("urllib.error").error.HTTPError(
                "https://api.notion.com",
                429,
                "blocked",
                {"Retry-After": "60"},
                io.BytesIO(b'{"code":"public_api_request_blocked"}'),
            )

        client = NotionClient("test-token", opener=opener)
        with self.assertRaisesRegex(NotionSyncError, "HTTP 429"):
            client.request("GET", "/pages/id")
        self.assertEqual(calls, 1)

    def test_asset_link_must_be_reachable_over_https(self):
        from .notion import verify_asset_url

        self.assertIsNone(
            verify_asset_url(
                "https://assets.example/model.glb",
                opener=lambda _request, timeout: _FakeResponse({"ok": True}),
            )
        )
        with self.assertRaisesRegex(NotionSyncError, "HTTP 403"):
            verify_asset_url(
                "https://assets.example/model.glb",
                opener=lambda _request, timeout: (_ for _ in ()).throw(
                    __import__("urllib.error").error.HTTPError(
                        "https://assets.example/model.glb",
                        403,
                        "forbidden",
                        {},
                        io.BytesIO(b""),
                    )
                ),
            )

    def test_page_append_is_idempotent(self):
        page_id = "abcdef0123456789abcdef0123456789"
        marker = f"ruff-3d-model:{page_id}"
        calls: list[str] = []
        added = False

        def opener(request, timeout):
            nonlocal added
            self.assertEqual(timeout, 20)
            calls.append(request.method)
            if request.method == "GET" and "/pages/" in request.full_url:
                return _FakeResponse({"object": "page"})
            if request.method == "GET":
                results = []
                if added:
                    results = [
                        {
                            "type": "paragraph",
                            "paragraph": {
                                "rich_text": [{"plain_text": marker}]
                            },
                        }
                    ]
                return _FakeResponse(
                    {"results": results, "has_more": False, "next_cursor": None}
                )
            added = True
            body = json.loads(request.data)
            self.assertEqual(len(body["children"]), 5)
            return _FakeResponse({"results": [{"id": "block"}]})

        client = NotionClient("test-token", opener=opener)
        first = client.sync_model(
            page_id,
            page_id,
            "Kreatur",
            f"https://assets.example/{page_id}/model.glb",
            "sf3d-test",
        )
        second = client.sync_model(
            page_id,
            page_id,
            "Kreatur",
            f"https://assets.example/{page_id}/model.glb",
            "sf3d-test",
        )
        self.assertTrue(first)
        self.assertFalse(second)
        self.assertEqual(calls.count("PATCH"), 1)

    def test_ambiguous_write_failure_is_not_retried_and_marker_recovers(self):
        import urllib.error

        page_id = "abcdef0123456789abcdef0123456789"
        marker = f"ruff-3d-model:{page_id}"
        added = False
        patch_calls = 0

        def opener(request, timeout):
            nonlocal added, patch_calls
            if request.method == "GET" and "/pages/" in request.full_url:
                return _FakeResponse({"object": "page"})
            if request.method == "GET":
                results = (
                    [
                        {
                            "type": "paragraph",
                            "paragraph": {
                                "rich_text": [{"plain_text": marker}]
                            },
                        }
                    ]
                    if added
                    else []
                )
                return _FakeResponse(
                    {"results": results, "has_more": False, "next_cursor": None}
                )
            patch_calls += 1
            added = True
            raise urllib.error.HTTPError(
                "https://api.notion.com",
                503,
                "possibly persisted",
                {},
                io.BytesIO(b'{"code":"service_unavailable"}'),
            )

        client = NotionClient("test-token", opener=opener)
        with self.assertRaisesRegex(NotionSyncError, "HTTP 503"):
            client.sync_model(
                page_id,
                page_id,
                "Kreatur",
                f"https://assets.example/{page_id}/model.glb",
                "sf3d-test",
            )
        self.assertEqual(patch_calls, 1)
        self.assertFalse(
            client.sync_model(
                page_id,
                page_id,
                "Kreatur",
                f"https://assets.example/{page_id}/model.glb",
                "sf3d-test",
            )
        )
        self.assertEqual(patch_calls, 1)

    def test_notion_api_errors_do_not_expose_token(self):
        token = "secret-notion-token"

        def opener(_request, timeout):
            raise __import__("urllib.error").error.HTTPError(
                "https://api.notion.com",
                403,
                "forbidden",
                {},
                io.BytesIO(b""),
            )

        client = NotionClient(token, opener=opener, max_retries=0)
        with self.assertRaises(NotionSyncError) as error:
            client.request("GET", "/pages/id")
        self.assertNotIn(token, str(error.exception))
        self.assertIn("HTTP 403", str(error.exception))


if __name__ == "__main__":
    unittest.main()
