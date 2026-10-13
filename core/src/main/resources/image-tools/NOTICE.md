# Bundled image tools

These are separate native programs, executed through ProcessBuilder. All four
platform directories are included in both the core Maven JAR and standalone JAR:
macOS ARM64, macOS x86_64, Windows x86_64, Linux x86_64 (glibc).
Linux ARM64, Windows ARM64, 32-bit JVMs and Alpine/musl are not included; provide
matching external tools through imageCompressionToolsDir if needed.

## Origins and licenses

| Tool | Version / origin | License |
| --- | --- | --- |
| OptiPNG | ARM Mac 0.7.7, Intel Mac 0.7.6, Linux 0.7.5, Windows 0.7.8; Imagemin optipng-bin commit bff4af0f3817ee8c31a1d708f0086095cfa43598 | zlib-style; bundled libpng/zlib notices included |
| jpegtran | Mac libjpeg-turbo 1.5.1, Linux 1.4.0, Windows 1.5.0; Imagemin jpegtran-bin commit 04c82c896f54082e011bdb19776a29f443a91412 | IJG/BSD-style; see included source notices |
| Gifsicle | Mac/Windows 1.93, Linux 1.92; Imagemin gifsicle-bin commit da45b6d92e6c8b08930bbfa2c38a41672e137e13 | GPL-2.0; 1.92 and 1.93 corresponding source archives included |
| cwebp/dwebp | libwebp 1.6.0; official Google WebP release archives | BSD-3-Clause; COPYING and PATENTS included |
| Linux zlib | 1.3.1; Debian zlib1g 1.3.dfsg+really1.3.1-1+b1 amd64 | zlib-style; copyright and source included |

Sources:
- https://github.com/imagemin/optipng-bin
- https://github.com/imagemin/jpegtran-bin
- https://github.com/imagemin/gifsicle-bin
- https://storage.googleapis.com/downloads.webmproject.org/releases/webp/
- https://deb.debian.org/debian/pool/main/z/zlib/

The source archives and original build scripts are in `sources/`, including the
Gifsicle source alongside every redistributed binary. Original license and
copyright files are under `licenses/`. The Imagemin wrapper MIT licenses do not
replace the upstream native programs' licenses.

macOS ARM64 OptiPNG/jpegtran/Gifsicle were built from the included sources
with Xcode CLI tools. OptiPNG uses the macOS system zlib; the other libraries are
linked statically. The native programs have no Homebrew paths or library
dependencies. Windows jpegtran's libjpeg-62.dll and Linux OptiPNG's libz.so.1 are
included and resolved locally. Other dependencies are operating-system libraries.

The mirrored prebuilt tools use different versions on different platforms; their
version numbers above describe the executables, not the npm wrapper version.
Compression output is not guaranteed to be identical across operating systems.

## Integrity and regeneration

`downloads.json` locks the SHA-256 of upstream downloads. Every platform's
`files.properties` lists the packaged binaries and checksums verified before
execution. Extraction is isolated per processing run and involves no network
access. Tool errors emit explicit warnings and retain original images while
packaging continues; further images using the failed tool are skipped for that
run. Task cancellation is still propagated.

Normal project builds use the checked-in resources and need no native compiler.
To regenerate them on an Apple Silicon Mac, install Xcode CLI tools, Python 3.9+
and developer-only autoconf/automake, then run:

```sh
python3 scripts/prepare-image-tools.py
```

The regeneration script and upstream build scripts are included in `sources/`.
Version or source changes require an explicit update of the script and download
lock, regeneration, and rerunning native tests on all supported platforms.
