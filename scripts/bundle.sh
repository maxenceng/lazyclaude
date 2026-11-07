#!/usr/bin/env bash
# Builds the desktop frontend's deb and AppImage around a jlink runtime linked from a pinned
# Temurin, so a local bundle is the one CI tests (ADR 0059). Reads TEMURIN_VERSION, TEMURIN_TAG
# and TEMURIN_SHA256 from the Makefile, and expects target/lazyclaude.jar already built.
set -euo pipefail

: "${TEMURIN_VERSION:?set by the Makefile}"
: "${TEMURIN_TAG:?set by the Makefile}"
: "${TEMURIN_SHA256:?set by the Makefile}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${ROOT}/target/bundle"
ARCHIVE="OpenJDK25U-jdk_x64_linux_hotspot_${TEMURIN_VERSION//+/_}.tar.gz"
URL="https://github.com/adoptium/temurin25-binaries/releases/download/jdk-${TEMURIN_TAG}/${ARCHIVE}"
MODULES="java.base,java.desktop,java.management,java.sql,java.xml,jdk.jfr"
STAGE="${WORK}/stage"
CONFIG="${WORK}/tauri.bundle.conf.json"

fail() { echo "bundle: $*" >&2; exit 1; }

for tool in curl sha256sum tar python3 pnpm cargo; do
	command -v "${tool}" >/dev/null 2>&1 || fail "${tool} is missing: make doctor lists what a bundle needs"
done
[[ -f "${ROOT}/target/lazyclaude.jar" ]] || fail "target/lazyclaude.jar is missing: run make package"

mkdir -p "${WORK}/download"
sha_of() { sha256sum "$1" | cut -d' ' -f1; }

# A kept archive is reused only while it still matches the pin; a mismatch is deleted and refused.
if [[ -f "${WORK}/download/${ARCHIVE}" && "$(sha_of "${WORK}/download/${ARCHIVE}")" != "${TEMURIN_SHA256}" ]]; then
	echo "bundle: the kept ${ARCHIVE} does not match the pinned SHA-256, downloading again"
	rm -f "${WORK}/download/${ARCHIVE}"
fi
if [[ ! -f "${WORK}/download/${ARCHIVE}" ]]; then
	echo "bundle: downloading ${URL}"
	curl -fsSL --retry 3 -o "${WORK}/download/${ARCHIVE}.part" "${URL}"
	got="$(sha_of "${WORK}/download/${ARCHIVE}.part")"
	if [[ "${got}" != "${TEMURIN_SHA256}" ]]; then
		rm -f "${WORK}/download/${ARCHIVE}.part"
		fail "SHA-256 mismatch for ${ARCHIVE}: pinned ${TEMURIN_SHA256}, got ${got}"
	fi
	mv "${WORK}/download/${ARCHIVE}.part" "${WORK}/download/${ARCHIVE}"
fi

JDK="${WORK}/jdk-${TEMURIN_VERSION}"
if [[ ! -x "${JDK}/bin/jlink" ]]; then
	rm -rf "${JDK}" "${JDK}.part"
	mkdir -p "${JDK}.part"
	tar -xzf "${WORK}/download/${ARCHIVE}" -C "${JDK}.part" --strip-components=1
	mv "${JDK}.part" "${JDK}"
fi

rm -rf "${STAGE}"
mkdir -p "${STAGE}/natives/linux/x86-64"
"${JDK}/bin/jlink" --add-modules "${MODULES}" --strip-debug --no-header-files --no-man-pages --output "${STAGE}/runtime"

# jlink leaves files read-only, which tauri-build could not overwrite when it copies them again.
chmod -R u+w "${STAGE}/runtime"

unpacked="${WORK}/jar-natives"
rm -rf "${unpacked}"
mkdir -p "${unpacked}"
(cd "${unpacked}" && "${JDK}/bin/jar" xf "${ROOT}/target/lazyclaude.jar" \
	resources/com/pty4j/native/linux/x86-64/libpty.so com/sun/jna/linux-x86-64/libjnidispatch.so)
cp "${unpacked}/resources/com/pty4j/native/linux/x86-64/libpty.so" "${STAGE}/natives/linux/x86-64/libpty.so"
cp "${unpacked}/com/sun/jna/linux-x86-64/libjnidispatch.so" "${STAGE}/natives/libjnidispatch.so"
cp "${ROOT}/target/lazyclaude.jar" "${STAGE}/lazyclaude.jar"

# The pom's version is the bundle's; read from the file so no Maven run is needed.
version="$(python3 -I - "${ROOT}/pom.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
print(ET.parse(sys.argv[1]).getroot().find("m:version", ns).text)
PY
)"

# Paths are relative to frontend/src-tauri, where tauri-build resolves resources.
cat > "${CONFIG}" <<JSON
{
  "version": "${version}",
  "bundle": {
    "active": true,
    "targets": ["deb", "appimage"],
    "resources": {
      "../../target/bundle/stage/runtime/": "runtime/",
      "../../target/bundle/stage/lazyclaude.jar": "lazyclaude.jar",
      "../../target/bundle/stage/natives/": "natives/"
    }
  }
}
JSON

cd "${ROOT}/frontend"
pnpm install --frozen-lockfile
rm -rf "${ROOT}/frontend/src-tauri/target/release/bundle"
# linuxdeploy resolves libjava.so's libjvm.so through the library path, which a JDK finds by itself.
# deferred: the AppImage carries libjvm.so a second time under usr/lib (25 MB) — close when linuxdeploy can be told libjvm.so is already in the AppDir
runtime_lib="${ROOT}/frontend/src-tauri/target/release/bundle/appimage/lazyclaude.AppDir/usr/lib/lazyclaude/runtime/lib"
LD_LIBRARY_PATH="${runtime_lib}/server:${runtime_lib}${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}" pnpm tauri build --config "${CONFIG}"

mkdir -p "${WORK}/out"
rm -f "${WORK}/out"/*
cp "${ROOT}"/frontend/src-tauri/target/release/bundle/deb/*.deb "${ROOT}"/frontend/src-tauri/target/release/bundle/appimage/*.AppImage "${WORK}/out/"
ls -l "${WORK}/out"
