#!/usr/bin/env python3
"""Prepare checked-in image tools. Requires Python 3; macOS ARM builds need Xcode CLI tools.

Downloads are pinned to immutable upstream commits / versioned releases. Existing
download hashes are enforced by downloads.json. No downloads occur at runtime.
"""
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "core/src/main/resources/image-tools"
WORK = ROOT / "build/image-tools-preparation"
REPOS = {
    "optipng": ("optipng-bin", "bff4af0f3817ee8c31a1d708f0086095cfa43598", "optipng.tar.gz"),
    "jpegtran": ("jpegtran-bin", "04c82c896f54082e011bdb19776a29f443a91412", "libjpeg-turbo-1.5.1.tar.gz"),
    "gifsicle": ("gifsicle-bin", "da45b6d92e6c8b08930bbfa2c38a41672e137e13", "gifsicle-1.93.tar.gz"),
}
WEBP = "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/"
MANIFEST = DEST / "downloads.json"
LOCK = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}
DOWNLOADS = dict(LOCK)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def download(url):
    cache = WORK / "downloads" / hashlib.sha256(url.encode()).hexdigest()
    cache.parent.mkdir(parents=True, exist_ok=True)
    if cache.exists():
        data = cache.read_bytes()
    else:
        print("Download", url, flush=True)
        with urllib.request.urlopen(url, timeout=90) as response:
            data = response.read()
        cache.write_bytes(data)
    if url in LOCK and LOCK[url] != sha(data):
        raise RuntimeError("Checksum mismatch: " + url)
    DOWNLOADS[url] = sha(data)
    return data


def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def github(tool, file):
    repo, commit, _ = REPOS[tool]
    return download(f"https://raw.githubusercontent.com/imagemin/{repo}/{commit}/{file}")


def run(args, cwd):
    log = WORK / "build.log"
    with log.open("ab") as output:
        env = dict(os.environ, CC="clang", CFLAGS="-O2 -std=c99 -DPNG_ARM_NEON_OPT=0 -mmacosx-version-min=11.0",
                   MACOSX_DEPLOYMENT_TARGET="11.0")
        result = subprocess.run(args, cwd=cwd, env=env, stdout=output, stderr=output)
    if result.returncode:
        raise RuntimeError(f"Failed {args}; see {log}")


def build_arm(tool, archive):
    target = WORK / (tool + "-arm64")
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)
    # Only trusted pinned archives; reject paths that would escape the workspace.
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as source:
        for member in source.getmembers():
            if member.issym() or member.islnk() or not (target / member.name).resolve().is_relative_to(target.resolve()):
                raise RuntimeError("Unsafe archive member: " + member.name)
        source.extractall(target)
    source = next(p for p in target.iterdir() if p.is_dir())
    print("Build macOS ARM64", tool, flush=True)
    options = {
        "optipng": ["--with-system-zlib"],
        "jpegtran": ["--disable-shared", "--without-simd", "--host=aarch64-apple-darwin"],
        "gifsicle": ["--disable-gifview", "--disable-gifdiff"],
    }
    if not (source / "configure").exists():
        run(["autoreconf", "-ivf"], source)
    run(["./configure"] + options[tool], source)
    run(["make", "-j4"], source)
    executable = next(p for p in source.rglob(tool) if p.is_file() and p.stat().st_mode & 0o111)
    output = DEST / "macos-arm64" / tool
    save(output, executable.read_bytes())
    # ARM builds must not retain paths to Homebrew libraries.
    deps = subprocess.check_output(["otool", "-L", str(output)], text=True)
    if "/opt/homebrew" in deps or "@rpath" in deps or "/usr/local" in deps:
        raise RuntimeError("Non-system dynamic library dependency: " + deps)


def main():
    WORK.mkdir(parents=True, exist_ok=True)
    for tool, (_, _, source_name) in REPOS.items():
        archive = github(tool, "vendor/source/" + source_name)
        save(DEST / "sources" / source_name, archive)
        save(DEST / "licenses" / (tool + "-wrapper.txt"), github(tool, "license"))
        save(DEST / "sources" / (tool + "-upstream-install.js"), github(tool, "lib/install.js"))
        with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as source:
            for member in source.getmembers():
                if member.isfile() and Path(member.name).name.lower() in {
                    "license", "license.txt", "copying", "copyright", "readme.ijg", "readme"
                }:
                    save(DEST / "licenses" / tool / member.name, source.extractfile(member).read())
        for os_id, vendor in [("macos-x86_64", "macos/"), ("linux-x86_64", "linux/x64/"), ("windows-x86_64", "win/x64/")]:
            if tool == "optipng" and os_id.startswith("windows"):
                vendor = "win/"
            name = tool + (".exe" if os_id.startswith("windows") else "")
            save(DEST / os_id / name, github(tool, "vendor/" + vendor + name))
            if tool == "jpegtran" and os_id.startswith("windows"):
                save(DEST / os_id / "libjpeg-62.dll", github(tool, "vendor/win/x64/libjpeg-62.dll"))
        if platform.system() == "Darwin" and platform.machine() == "arm64":
            build_arm(tool, archive)
        elif not (DEST / "macos-arm64" / tool).exists():
            raise RuntimeError("Generate ARM64 binaries on an Apple Silicon Mac before packaging")
    for os_id, package in [
        ("macos-arm64", "libwebp-1.6.0-mac-arm64.tar.gz"),
        ("macos-x86_64", "libwebp-1.6.0-mac-x86-64.tar.gz"),
        ("linux-x86_64", "libwebp-1.6.0-linux-x86-64.tar.gz"),
        ("windows-x86_64", "libwebp-1.6.0-windows-x64-no-wic.zip"),
    ]:
        data = download(WEBP + package)
        if package.endswith(".zip"):
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                for name in archive.namelist():
                    if name.endswith("/bin/cwebp.exe") or name.endswith("/bin/dwebp.exe"):
                        save(DEST / os_id / Path(name).name, archive.read(name))
        else:
            with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as archive:
                for member in archive.getmembers():
                    if member.isfile() and (member.name.endswith("/bin/cwebp") or member.name.endswith("/bin/dwebp")):
                        save(DEST / os_id / Path(member.name).name, archive.extractfile(member).read())
    archive = download(WEBP + "libwebp-1.6.0.tar.gz")
    save(DEST / "sources/libwebp-1.6.0.tar.gz", archive)
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as source:
        for member in source.getmembers():
            if member.isfile() and Path(member.name).name in {"COPYING", "PATENTS"}:
                save(DEST / "licenses/webp" / Path(member.name).name, source.extractfile(member).read())
    # The Linux OptiPNG executable links zlib; package it instead of requiring a system install.
    debian = "https://deb.debian.org/debian/pool/main/z/zlib/"
    package = download(debian + "zlib1g_1.3.dfsg+really1.3.1-1+b1_amd64.deb")
    offset = 8  # Debian packages use the ar archive format.
    while offset + 60 <= len(package):
        header = package[offset:offset + 60]
        length = int(header[48:58])
        name = header[:16].decode().strip().rstrip("/")
        data = package[offset + 60:offset + 60 + length]
        offset += 60 + length + (length & 1)
        if name.startswith("data.tar"):
            with tarfile.open(fileobj=io.BytesIO(data), mode="r:*") as archive:
                for member in archive.getmembers():
                    if member.isfile() and Path(member.name).name.startswith("libz.so.1."):
                        save(DEST / "linux-x86_64/libz.so.1", archive.extractfile(member).read())
                    if member.isfile() and member.name.endswith("/zlib1g/copyright"):
                        save(DEST / "licenses/zlib/copyright", archive.extractfile(member).read())
    if not (DEST / "linux-x86_64/libz.so.1").exists():
        raise RuntimeError("Missing Linux zlib in Debian package")
    for name in ["zlib_1.3.dfsg+really1.3.1.orig.tar.gz", "zlib_1.3.dfsg+really1.3.1-1.debian.tar.xz"]:
        save(DEST / "sources" / name, download(debian + name))
    # Imagemin's Linux Gifsicle binary is 1.92, while its other platform binaries are 1.93.
    save(DEST / "sources/gifsicle-1.92.tar.gz", download("https://www.lcdf.org/gifsicle/gifsicle-1.92.tar.gz"))
    # Do not distribute the unrelated GLUT DLL from WebP's viewer package.
    (DEST / "windows-x86_64/freeglut.dll").unlink(missing_ok=True)
    for os_id in ["macos-arm64", "macos-x86_64", "linux-x86_64", "windows-x86_64"]:
        directory = DEST / os_id
        entries = []
        for file in sorted(directory.iterdir()):
            if file.name == "files.properties":
                continue
            entries.append(file.name + "=" + sha(file.read_bytes()))
            if not file.name.endswith(".dll"):
                file.chmod(0o755)
        save(directory / "files.properties", ("\n".join(entries) + "\n").encode())
    save(MANIFEST, (json.dumps(DOWNLOADS, indent=2, sort_keys=True) + "\n").encode())
    save(DEST / "sources/prepare-image-tools.py", Path(__file__).read_bytes())
    print("Prepared bundled tools:", DEST)


if __name__ == "__main__":
    main()
