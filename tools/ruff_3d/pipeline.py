#!/usr/bin/env python3
"""GPU-gated Stable Fast 3D execution and structural GLB validation."""

from __future__ import annotations

import hashlib
import importlib
import json
import os
import platform
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

from .manifest import save_manifest


SF3D_REPOSITORY = "https://github.com/Stability-AI/stable-fast-3d"
SF3D_COMMIT = "ff21fc491b4dc5314bf6734c7c0dabd86b5f5bb2"  # pragma: allowlist secret
SF3D_MODEL_ID = "stabilityai/stable-fast-3d"
SF3D_WEIGHTS_REVISION = (
    "f0c9a8ffd62cb1bbc8a7a53c9f87a0be1b6be778"  # pragma: allowlist secret
)
GLB_JSON_CHUNK = b"JSON"
GLB_BIN_CHUNK = b"BIN\x00"


class PipelineError(RuntimeError):
    """Raised when a generation or model validation step cannot succeed."""


def _repository_root() -> Path:
    return Path(__file__).resolve().parents[2]


def load_config(
    path: Path | None = None,
    repository_root: Path | None = None,
) -> dict[str, Any]:
    root = repository_root or _repository_root()
    config_path = path or root / "tools" / "ruff_3d" / "config.json"
    config = json.loads(config_path.read_text(encoding="utf-8"))
    config["sf3d_repo"] = os.environ.get(
        "SF3D_REPO_DIR", config.get("sf3d_repo")
    )
    config["python_executable"] = os.environ.get(
        "SF3D_PYTHON", config.get("python_executable")
    )
    config["pretrained_model"] = os.environ.get(
        "SF3D_PRETRAINED_MODEL", config.get("pretrained_model", SF3D_MODEL_ID)
    )
    config["output_dir"] = os.environ.get(
        "RUFF_3D_MODEL_DIR", config.get("output_dir", ".artifacts/3d/models")
    )
    config["log_dir"] = os.environ.get(
        "RUFF_3D_LOG_DIR", config.get("log_dir", ".artifacts/3d/logs")
    )
    config["asset_base_url"] = os.environ.get(
        "RUFF_3D_ASSET_BASE_URL", config.get("asset_base_url")
    )
    return config


def _resolve_path(root: Path, value: str | None) -> Path | None:
    if not value:
        return None
    path = Path(value).expanduser()
    return path.resolve() if path.is_absolute() else (root / path).resolve()


def _system_memory_bytes() -> int | None:
    try:
        pages = os.sysconf("SC_PHYS_PAGES")
        page_size = os.sysconf("SC_PAGE_SIZE")
    except (AttributeError, OSError, ValueError):
        return None
    return int(pages * page_size)


def _gpu_report() -> dict[str, Any]:
    report: dict[str, Any] = {
        "available": False,
        "kind": "none",
        "name": None,
        "vram_bytes": None,
        "torch_available": False,
        "details": None,
    }
    try:
        torch = importlib.import_module("torch")

        report["torch_available"] = True
        if torch.cuda.is_available():
            properties = torch.cuda.get_device_properties(0)
            report.update(
                available=True,
                kind="cuda",
                name=properties.name,
                vram_bytes=int(properties.total_memory),
            )
            return report
        mps = getattr(torch.backends, "mps", None)
        if mps is not None and mps.is_available():
            report.update(available=True, kind="mps", name="Apple MPS")
            return report
    except ImportError:
        report["details"] = "PyTorch is not installed."
    except (RuntimeError, OSError) as error:
        report["details"] = f"PyTorch GPU probe failed: {error}"

    nvidia_smi = shutil.which("nvidia-smi")
    if nvidia_smi:
        try:
            result = subprocess.run(
                [
                    nvidia_smi,
                    "--query-gpu=name,memory.total",
                    "--format=csv,noheader,nounits",
                ],
                check=False,
                capture_output=True,
                text=True,
                timeout=5,
            )
        except (OSError, subprocess.TimeoutExpired) as error:
            report["details"] = f"nvidia-smi probe failed: {type(error).__name__}."
            return report
        if result.returncode == 0 and result.stdout.strip():
            first = result.stdout.splitlines()[0].split(",", maxsplit=1)
            report.update(
                name=first[0].strip(),
                details="NVIDIA hardware found, but PyTorch CUDA is unavailable."
                if not report["torch_available"]
                else report["details"],
            )
    return report


def _network_probe(timeout: float = 5.0) -> dict[str, Any]:
    request = urllib.request.Request(
        "https://api.github.com/repos/Stability-AI/stable-fast-3d",
        headers={"User-Agent": "Ruff-SF3D-preflight"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return {
                "available": 200 <= response.status < 400,
                "status": response.status,
            }
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        return {"available": False, "error": f"{type(error).__name__}: {error}"}


def _weights_snapshot(pretrained_model: str, revision: str) -> Path | None:
    local_model = Path(pretrained_model).expanduser()
    if local_model.is_dir():
        snapshot = local_model.resolve()
    elif pretrained_model == SF3D_MODEL_ID:
        cache_root_value = (
            os.environ.get("HF_HUB_CACHE")
            or os.environ.get("HUGGINGFACE_HUB_CACHE")
            or str(
                Path(
                    os.environ.get(
                        "HF_HOME",
                        Path.home() / ".cache" / "huggingface",
                    )
                )
                / "hub"
            )
        )
        snapshot = (
            Path(cache_root_value)
            / "models--stabilityai--stable-fast-3d"
            / "snapshots"
            / revision
        ).resolve()
    else:
        return None
    if snapshot.name != revision:
        return None
    if not (snapshot / "config.yaml").is_file():
        return None
    if not (snapshot / "model.safetensors").is_file():
        return None
    return snapshot


def preflight(
    config: dict[str, Any],
    repository_root: Path | None = None,
    network_probe: Any = _network_probe,
    gpu_probe: Any = _gpu_report,
) -> dict[str, Any]:
    root = (repository_root or _repository_root()).resolve()
    output_dir = _resolve_path(root, config.get("output_dir"))
    sf3d_repo = _resolve_path(root, config.get("sf3d_repo"))
    run_script = sf3d_repo / "run.py" if sf3d_repo else None
    pretrained_model = str(config.get("pretrained_model", SF3D_MODEL_ID))
    weights_revision = SF3D_WEIGHTS_REVISION
    weights_snapshot = _weights_snapshot(pretrained_model, weights_revision)

    disk_path = output_dir or root
    while not disk_path.exists() and disk_path != disk_path.parent:
        disk_path = disk_path.parent
    disk = shutil.disk_usage(disk_path)
    write_access = False
    if output_dir is not None:
        try:
            output_dir.mkdir(parents=True, exist_ok=True)
            with tempfile.NamedTemporaryFile(
                dir=output_dir,
                prefix=".ruff-write-test-",
            ):
                pass
            write_access = True
        except OSError:
            write_access = False
    validator_setting = str(
        config.get("gltf_validator_executable", "gltf_validator")
    )
    validator_path = Path(validator_setting).expanduser()
    if not validator_path.is_absolute():
        validator_path = root / validator_path

    installed_commit = None
    if sf3d_repo is not None and sf3d_repo.is_dir():
        result = subprocess.run(
            ["git", "-C", str(sf3d_repo), "rev-parse", "HEAD"],
            check=False,
            capture_output=True,
            text=True,
            timeout=5,
        )
        if result.returncode == 0:
            installed_commit = result.stdout.strip()
    python_executable = config.get("python_executable") or sys.executable
    dependency_check: dict[str, Any] = {
        "interpreter": str(python_executable),
        "run_py_imports_work": False,
        "details": None,
    }
    if run_script and run_script.is_file():
        try:
            result = subprocess.run(
                [str(python_executable), str(run_script), "--help"],
                check=False,
                capture_output=True,
                text=True,
                timeout=30,
                env=_sf3d_environment(),
            )
            dependency_check["run_py_imports_work"] = result.returncode == 0
            if result.returncode != 0:
                dependency_check["details"] = (
                    result.stderr[-2000:] or f"Exit status {result.returncode}."
                )
        except (OSError, subprocess.TimeoutExpired) as error:
            dependency_check["details"] = f"{type(error).__name__}: {error}"

    return {
        "python": platform.python_version(),
        "operating_system": platform.platform(),
        "architecture": platform.machine(),
        "memory_total_bytes": _system_memory_bytes(),
        "gpu": gpu_probe(),
        "free_disk_bytes": disk.free,
        "disk_total_bytes": disk.total,
        "output_directory": str(output_dir) if output_dir else None,
        "output_writable": write_access,
        "sf3d_repository": str(sf3d_repo) if sf3d_repo else None,
        "sf3d_run_script_found": bool(run_script and run_script.is_file()),
        "gltf_validator_found": bool(
            shutil.which(validator_setting) or validator_path.is_file()
        ),
        "pinned_code_commit": SF3D_COMMIT,
        "installed_code_commit": installed_commit,
        "code_commit_matches_pin": installed_commit == SF3D_COMMIT,
        "license_reviewed": bool(config.get("license_reviewed", False)),
        "dependencies": dependency_check,
        "pretrained_model": pretrained_model,
        "weights_revision": weights_revision,
        "weights_snapshot_path": (
            str(weights_snapshot) if weights_snapshot is not None else None
        ),
        "weights_available_locally": weights_snapshot is not None,
        "huggingface_token_configured": bool(
            os.environ.get("HF_TOKEN") or os.environ.get("HUGGINGFACE_HUB_TOKEN")
        ),
        "network": network_probe(),
    }


def _read_glb(path: Path) -> tuple[dict[str, Any], bytes]:
    try:
        data = path.read_bytes()
    except OSError as error:
        raise PipelineError(f"Cannot read GLB file: {error}") from error
    if len(data) < 20:
        raise PipelineError("GLB is too small to contain a header and JSON chunk.")
    magic, version, declared_length = struct.unpack_from("<4sII", data, 0)
    if magic != b"glTF":
        raise PipelineError("Invalid GLB magic; expected 'glTF'.")
    if version != 2:
        raise PipelineError(f"Unsupported GLB version {version}; expected 2.")
    if declared_length != len(data):
        raise PipelineError("GLB header length does not match the file size.")

    chunks: list[tuple[bytes, bytes]] = []
    offset = 12
    while offset < len(data):
        if offset + 8 > len(data):
            raise PipelineError("Truncated GLB chunk header.")
        chunk_length, chunk_type = struct.unpack_from("<I4s", data, offset)
        offset += 8
        end = offset + chunk_length
        if end > len(data) or chunk_length % 4:
            raise PipelineError("GLB chunk has an invalid length.")
        chunks.append((chunk_type, data[offset:end]))
        offset = end
    if not chunks or chunks[0][0] != GLB_JSON_CHUNK:
        raise PipelineError("The first GLB chunk must contain JSON.")
    if any(
        chunk_type not in {GLB_JSON_CHUNK, GLB_BIN_CHUNK}
        for chunk_type, _ in chunks
    ):
        raise PipelineError("GLB contains an unknown chunk type.")
    if sum(chunk_type == GLB_JSON_CHUNK for chunk_type, _ in chunks) != 1:
        raise PipelineError("GLB must contain exactly one JSON chunk.")
    if sum(chunk_type == GLB_BIN_CHUNK for chunk_type, _ in chunks) > 1:
        raise PipelineError("GLB may contain at most one BIN chunk.")

    try:
        document = json.loads(chunks[0][1].rstrip(b" \t\r\n\x00").decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise PipelineError(f"GLB JSON chunk is invalid: {error}") from error
    binary = next((chunk for kind, chunk in chunks if kind == GLB_BIN_CHUNK), b"")
    return document, binary


def validate_glb(
    path: Path,
    validator_executable: str | None = None,
    require_external: bool = False,
) -> dict[str, Any]:
    if not path.is_file():
        raise PipelineError("GLB output does not exist.")
    size = path.stat().st_size
    if size <= 0:
        raise PipelineError("GLB output is empty.")
    document, binary = _read_glb(path)
    asset = document.get("asset")
    if not isinstance(asset, dict) or asset.get("version") != "2.0":
        raise PipelineError("GLB must declare glTF asset.version 2.0.")

    meshes = document.get("meshes")
    accessors = document.get("accessors", [])
    if not isinstance(meshes, list) or not meshes:
        raise PipelineError("GLB contains no meshes.")
    primitive_count = 0
    for mesh in meshes:
        primitives = mesh.get("primitives", []) if isinstance(mesh, dict) else []
        if not primitives:
            raise PipelineError("GLB contains a mesh with no primitives.")
        for primitive in primitives:
            attributes = primitive.get("attributes", {})
            position_index = attributes.get("POSITION")
            if not isinstance(position_index, int) or not (
                0 <= position_index < len(accessors)
            ):
                raise PipelineError("Mesh primitive has no valid POSITION accessor.")
            position = accessors[position_index]
            if position.get("type") != "VEC3" or position.get("count", 0) < 3:
                raise PipelineError("POSITION accessor is not a non-empty VEC3 array.")
            if position.get("componentType") != 5126:
                raise PipelineError("POSITION accessor must use float32 components.")
            indices_index = primitive.get("indices")
            if indices_index is not None:
                if not isinstance(indices_index, int) or not (
                    0 <= indices_index < len(accessors)
                ):
                    raise PipelineError(
                        "Mesh primitive has an invalid indices accessor."
                    )
                if accessors[indices_index].get("count", 0) < 3:
                    raise PipelineError(
                        "Mesh indices accessor contains fewer than 3 values."
                    )
            primitive_count += 1

    buffers = document.get("buffers", [])
    if buffers:
        if len(buffers) != 1 or buffers[0].get("uri") is not None:
            raise PipelineError("GLB must use one embedded buffer, not external URIs.")
        if buffers[0].get("byteLength", 0) > len(binary):
            raise PipelineError(
                "GLB binary chunk is shorter than its buffer.byteLength."
            )
    elif binary:
        raise PipelineError("GLB has a BIN chunk but no buffer declaration.")

    buffer_views = document.get("bufferViews", [])
    for view in buffer_views:
        if view.get("buffer") != 0:
            raise PipelineError("GLB bufferViews must refer to the embedded buffer.")
        end = view.get("byteOffset", 0) + view.get("byteLength", 0)
        if end > len(binary):
            raise PipelineError("GLB bufferView extends beyond its binary chunk.")

    images = document.get("images", [])
    for image in images:
        if "bufferView" not in image and not str(image.get("uri", "")).startswith(
            "data:"
        ):
            raise PipelineError("GLB contains an image that is not embedded.")

    materials = document.get("materials", [])
    if not materials:
        raise PipelineError("GLB contains no materials.")
    details: dict[str, Any] = {
        "file_size_bytes": size,
        "mesh_count": len(meshes),
        "primitive_count": primitive_count,
        "material_count": len(materials),
        "texture_count": len(document.get("textures", [])),
        "image_count": len(images),
        "glb_version": 2,
    }
    if require_external:
        executable = (
            shutil.which(validator_executable) if validator_executable else None
        )
        if (
            executable is None
            and validator_executable
            and Path(validator_executable).is_file()
        ):
            executable = str(Path(validator_executable).resolve())
        if executable is None:
            raise PipelineError(
                "Khronos gltf_validator is required but was not found on PATH."
            )
        try:
            result = subprocess.run(
                [executable, "--stdout", str(path.resolve())],
                check=False,
                capture_output=True,
                text=True,
                timeout=60,
            )
        except (OSError, subprocess.TimeoutExpired) as error:
            raise PipelineError(
                f"Khronos glTF validation could not run: {type(error).__name__}."
            ) from error
        try:
            report = json.loads(result.stdout)
        except json.JSONDecodeError as error:
            raise PipelineError(
                "Khronos gltf_validator did not return a JSON report."
            ) from error
        issues = report.get("issues", {})
        errors = issues.get("numErrors")
        if result.returncode != 0 or not isinstance(errors, int) or errors != 0:
            raise PipelineError(
                f"Khronos gltf_validator reported {errors!r} errors."
            )
        details["external_validator"] = "Khronos gltf_validator"
        details["external_validator_errors"] = errors
        details["external_validator_warnings"] = issues.get("numWarnings", 0)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    details["sha256"] = digest
    return details


def _redact(text: str) -> str:
    for secret in (
        os.environ.get("HF_TOKEN"),
        os.environ.get("HUGGINGFACE_HUB_TOKEN"),
        os.environ.get("NOTION_TOKEN"),
    ):
        if secret:
            text = text.replace(secret, "[REDACTED]")
    return text[-12000:]


def _sf3d_environment() -> dict[str, str]:
    environment = os.environ.copy()
    for name in tuple(environment):
        upper_name = name.upper()
        sensitive = any(
            marker in upper_name
            for marker in ("TOKEN", "SECRET", "PASSWORD", "API_KEY", "CREDENTIAL")
        )
        if sensitive:
            environment.pop(name, None)
    return environment


def _write_process_log(log_path: Path, stdout: str, stderr: str) -> None:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(
        f"--- stdout ---\n{_redact(stdout)}\n--- stderr ---\n{_redact(stderr)}\n",
        encoding="utf-8",
    )


def _relative_or_absolute(path: Path, repository_root: Path) -> str:
    resolved = path.resolve()
    try:
        return resolved.relative_to(repository_root.resolve()).as_posix()
    except ValueError:
        return str(resolved)


def _reusable_model(
    model_path: Path, config: dict[str, Any]
) -> dict[str, Any] | None:
    if not model_path.is_file():
        return None
    try:
        details = validate_glb(
            model_path,
            validator_executable=config.get("gltf_validator_executable"),
            require_external=bool(config.get("require_external_glb_validation", True)),
        )
    except PipelineError:
        return None
    return details


def _reusable_manifest_model(
    entry: dict[str, Any],
    model_path: Path,
    config: dict[str, Any],
    repository_root: Path,
) -> dict[str, Any] | None:
    recorded_path = _resolve_path(repository_root, entry.get("model_path"))
    if (
        recorded_path is None
        or recorded_path != model_path.resolve()
        or entry.get("status")
        not in {"generated", "validating", "validated", "notion_synced"}
    ):
        return None
    details = _reusable_model(model_path, config)
    if details is None:
        return None
    expected_hash = entry.get("model_sha256")
    if expected_hash is not None and details["sha256"] != expected_hash:
        return None
    return details


def _model_version() -> str:
    return f"{SF3D_MODEL_ID}@{SF3D_WEIGHTS_REVISION}"


def generate_one(
    manifest: dict[str, Any],
    entry: dict[str, Any],
    config: dict[str, Any],
    repository_root: Path | None = None,
    gpu_available: bool | None = None,
    allow_cpu: bool = False,
) -> str:
    root = (repository_root or _repository_root()).resolve()
    output_dir = _resolve_path(root, config.get("output_dir"))
    log_dir = _resolve_path(root, config.get("log_dir"))
    if output_dir is None or log_dir is None:
        raise PipelineError("Model output and log directories must be configured.")
    model_path = output_dir / entry["creature_id"] / "model.glb"

    reusable = _reusable_manifest_model(entry, model_path, config, root)
    if reusable is not None:
        final_status = (
            "notion_synced" if entry["status"] == "notion_synced" else "validated"
        )
        entry.update(
            status=final_status,
            generation_status="generated",
            model_path=_relative_or_absolute(model_path, root),
            model_format="glb",
            model_size_bytes=reusable["file_size_bytes"],
            model_sha256=reusable["sha256"],
            model_version=entry.get("model_version") or _model_version(),
            validation_status="validated",
            validation_details=reusable,
            error=None,
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return final_status

    image_path = entry.get("reference_image")
    if not image_path:
        entry.update(
            status="needs_reference_image",
            generation_status="needs_reference_image",
            error="No verified creature-specific reference image is available.",
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    resolved_image = (root / image_path).resolve()
    if not resolved_image.is_relative_to(root) or not resolved_image.is_file():
        entry.update(
            status="failed",
            generation_status="failed",
            error="Configured reference image is missing or outside the repository.",
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    if gpu_available is None:
        gpu_available = bool(_gpu_report()["available"])
    if not gpu_available and not allow_cpu:
        entry.update(
            status="blocked_no_gpu",
            generation_status="blocked_no_gpu",
            error=(
                "Local Stable Fast 3D inference was not started: no CUDA/MPS "
                "GPU is available. Configure an authorized GPU worker."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    if not config.get("license_reviewed", False):
        entry.update(
            status="failed",
            generation_status="failed",
            error=(
                "Generation is blocked until a person reviews and records the "
                "Stable Fast 3D code and model license terms."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    sf3d_repo = _resolve_path(root, config.get("sf3d_repo"))
    python_executable = config.get("python_executable") or sys.executable
    run_script = sf3d_repo / "run.py" if sf3d_repo else None
    if run_script is None or not run_script.is_file():
        entry.update(
            status="failed",
            generation_status="failed",
            error=(
                "Stable Fast 3D source is not configured. Set SF3D_REPO_DIR "
                "to an authorized checkout of the pinned official repository."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    try:
        commit_result = subprocess.run(
            ["git", "-C", str(sf3d_repo), "rev-parse", "HEAD"],
            check=False,
            capture_output=True,
            text=True,
            timeout=5,
        )
    except (OSError, subprocess.TimeoutExpired) as error:
        entry.update(
            status="failed",
            generation_status="failed",
            error=f"Could not verify the pinned Stable Fast 3D checkout: {error}",
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]
    expected_commit = str(config.get("sf3d_commit", SF3D_COMMIT))
    if commit_result.returncode != 0 or commit_result.stdout.strip() != expected_commit:
        entry.update(
            status="failed",
            generation_status="failed",
            error=(
                "Stable Fast 3D checkout is not the configured pinned commit "
                f"{expected_commit}."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    weights_snapshot = _weights_snapshot(
        str(config.get("pretrained_model", SF3D_MODEL_ID)),
        SF3D_WEIGHTS_REVISION,
    )
    if weights_snapshot is None:
        entry.update(
            status="failed",
            generation_status="failed",
            error=(
                "The pinned Stable Fast 3D weights are unavailable. Download "
                f"revision {SF3D_WEIGHTS_REVISION} and configure "
                "pretrained_model to its local snapshot directory."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    attempts_allowed = int(config.get("max_attempts", 2))
    if entry["attempt_count"] >= attempts_allowed:
        entry.update(
            status="failed",
            generation_status="failed",
            error=(
                f"Retry limit reached ({attempts_allowed}); use --retry-failed "
                "to explicitly reset this entry."
            ),
            updated_at=_utc_now(),
        )
        save_manifest(Path(config["manifest_path"]), manifest)
        return entry["status"]

    output_dir.mkdir(parents=True, exist_ok=True)
    model_path.parent.mkdir(parents=True, exist_ok=True)
    entry.update(
        status="generating",
        generation_status="generating",
        validation_status="not_started",
        error=None,
        updated_at=_utc_now(),
    )
    save_manifest(Path(config["manifest_path"]), manifest)

    final_error = "Stable Fast 3D did not produce a validated GLB."
    while entry["attempt_count"] < attempts_allowed:
        entry["attempt_count"] += 1
        entry["updated_at"] = _utc_now()
        save_manifest(Path(config["manifest_path"]), manifest)
        temporary_root = output_dir / ".temporary"
        temporary_root.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(
            prefix=f"{entry['creature_id']}-{entry['attempt_count']}-",
            dir=temporary_root,
        ) as temporary_name:
            temporary_output = Path(temporary_name) / "output"
            command = [
                str(python_executable),
                str(run_script),
                str(resolved_image),
                "--output-dir",
                str(temporary_output),
                "--texture-resolution",
                str(int(config.get("texture_resolution", 1024))),
                "--remesh_option",
                str(config.get("remesh_option", "none")),
                "--target_vertex_count",
                str(int(config.get("target_vertex_count", -1))),
                "--batch_size",
                "1",
            ]
            command.extend(["--pretrained-model", str(weights_snapshot)])
            environment = _sf3d_environment()
            try:
                result = subprocess.run(
                    command,
                    check=False,
                    capture_output=True,
                    text=True,
                    timeout=int(config.get("timeout_seconds", 900)),
                    env=environment,
                )
                _write_process_log(
                    log_dir
                    / f"{entry['creature_id']}-attempt-{entry['attempt_count']}.log",
                    result.stdout,
                    result.stderr,
                )
                if result.returncode != 0:
                    final_error = (
                        f"Stable Fast 3D exited with status {result.returncode}; "
                        f"see ignored log file for attempt {entry['attempt_count']}."
                    )
                else:
                    candidates = list(temporary_output.glob("0/mesh.glb"))
                    if not candidates:
                        final_error = (
                            "Stable Fast 3D exited successfully but did not create "
                            "the expected 0/mesh.glb file."
                        )
                    else:
                        candidate = candidates[0]
                        entry.update(
                            status="validating",
                            generation_status="generated",
                            model_path=_relative_or_absolute(model_path, root),
                            model_format="glb",
                            validation_status="validating",
                            updated_at=_utc_now(),
                        )
                        save_manifest(Path(config["manifest_path"]), manifest)
                        details = validate_glb(
                            candidate,
                            validator_executable=config.get(
                                "gltf_validator_executable"
                            ),
                            require_external=bool(
                                config.get("require_external_glb_validation", True)
                            ),
                        )
                        os.replace(candidate, model_path)
                        details = validate_glb(
                            model_path,
                            validator_executable=config.get(
                                "gltf_validator_executable"
                            ),
                            require_external=bool(
                                config.get("require_external_glb_validation", True)
                            ),
                        )
                        entry.update(
                            status="validated",
                            generation_status="generated",
                            model_path=_relative_or_absolute(model_path, root),
                            model_format="glb",
                            model_size_bytes=details["file_size_bytes"],
                            model_sha256=details["sha256"],
                            model_version=_model_version(),
                            generated_at=entry.get("generated_at") or _utc_now(),
                            validation_status="validated",
                            validation_details=details,
                            error=None,
                            updated_at=_utc_now(),
                        )
                        save_manifest(Path(config["manifest_path"]), manifest)
                        return "validated"
            except subprocess.TimeoutExpired as error:
                stdout = error.stdout or ""
                stderr = error.stderr or ""
                if isinstance(stdout, bytes):
                    stdout = stdout.decode(errors="replace")
                if isinstance(stderr, bytes):
                    stderr = stderr.decode(errors="replace")
                _write_process_log(
                    log_dir
                    / f"{entry['creature_id']}-attempt-{entry['attempt_count']}.log",
                    stdout,
                    stderr,
                )
                final_error = (
                    f"Stable Fast 3D timed out after "
                    f"{config.get('timeout_seconds', 900)} seconds."
                )
            except (OSError, PipelineError) as error:
                final_error = str(error)

        if entry["attempt_count"] < attempts_allowed:
            time.sleep(float(config.get("retry_delay_seconds", 5)))

    entry.update(
        status="failed",
        generation_status="failed",
        validation_status=(
            "failed" if entry["generation_status"] == "generated" else "not_started"
        ),
        error=final_error,
        updated_at=_utc_now(),
    )
    save_manifest(Path(config["manifest_path"]), manifest)
    return "failed"


def _utc_now() -> str:
    from datetime import datetime, timezone

    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def run_batch(
    manifest: dict[str, Any],
    config: dict[str, Any],
    repository_root: Path | None = None,
    limit: int | None = None,
    retry_failed: bool = False,
    allow_cpu: bool = False,
) -> dict[str, int]:
    root = (repository_root or _repository_root()).resolve()
    if int(config.get("parallelism", 1)) != 1:
        raise PipelineError("Only sequential execution (parallelism=1) is supported.")
    manifest_path = Path(config["manifest_path"])
    if not manifest_path.is_absolute():
        manifest_path = root / manifest_path

    if retry_failed:
        for entry in manifest["creatures"]:
            if entry["status"] == "failed":
                entry.update(
                    status=(
                        "queued"
                        if entry.get("reference_image")
                        else "needs_reference_image"
                    ),
                    generation_status=(
                        "queued"
                        if entry.get("reference_image")
                        else "needs_reference_image"
                    ),
                    validation_status="not_started",
                    attempt_count=0,
                    error=(
                        None
                        if entry.get("reference_image")
                        else (
                            "No verified creature-specific reference image "
                            "is available."
                        )
                    ),
                    updated_at=_utc_now(),
                )
        save_manifest(manifest_path, manifest)

    for entry in manifest["creatures"]:
        if entry["status"] in {"generating", "generated", "validating"}:
            model_path = _resolve_path(root, entry.get("model_path"))
            reusable = (
                _reusable_model(model_path, config)
                if model_path is not None
                else None
            )
            if model_path is not None and reusable is not None:
                entry["status"] = "validated"
                entry["generation_status"] = "generated"
                entry["validation_status"] = "validated"
                entry["model_path"] = _relative_or_absolute(model_path, root)
                entry["model_format"] = "glb"
                entry["model_size_bytes"] = reusable["file_size_bytes"]
                entry["model_sha256"] = reusable["sha256"]
                entry["model_version"] = (
                    entry.get("model_version") or _model_version()
                )
                entry["validation_details"] = reusable
                entry["updated_at"] = _utc_now()
            elif entry["attempt_count"] < int(config.get("max_attempts", 2)):
                entry["status"] = (
                    "queued"
                    if entry.get("reference_image")
                    else "needs_reference_image"
                )
                entry["generation_status"] = entry["status"]
                entry["validation_status"] = "not_started"
                entry["error"] = "Recovered an interrupted generation attempt."
                entry["updated_at"] = _utc_now()
            else:
                entry["status"] = "failed"
                entry["generation_status"] = "failed"
                entry["validation_status"] = "failed"
                entry["error"] = (
                    "Interrupted after reaching the configured attempt limit; "
                    "use --retry-failed to explicitly reset this entry."
                )
                entry["updated_at"] = _utc_now()
    save_manifest(manifest_path, manifest)

    gpu_available = bool(_gpu_report()["available"])
    selected = [
        entry
        for entry in manifest["creatures"]
        if entry["status"]
        in {
            "queued",
            "blocked_no_gpu",
            "failed",
            "generating",
            "generated",
            "validating",
        }
    ]
    if limit is not None:
        selected = selected[:limit]
    counts: dict[str, int] = {}
    for entry in selected:
        status = generate_one(
            manifest,
            entry,
            {**config, "manifest_path": str(manifest_path)},
            repository_root=root,
            gpu_available=gpu_available,
            allow_cpu=allow_cpu,
        )
        counts[status] = counts.get(status, 0) + 1
    return counts
