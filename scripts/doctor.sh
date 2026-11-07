#!/usr/bin/env bash
# Reports toolchain and project-setup problems as actionable instructions rather than as
# build failures. Nothing here fails a fresh project — a scaffold that greets you with a
# red build gets deleted rather than fixed.
set -uo pipefail

status=0

report() { printf '  %-8s %s\n' "$1" "$2"; }

echo "Toolchain"

# Same search order as the Makefile: an already-correct JAVA_HOME, then ~/.jdks, then
# the system path, then whatever is on PATH.
javac_bin=""
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" ]]; then
	javac_bin="${JAVA_HOME}/bin/javac"
else
	for candidate in "${HOME}"/.jdks/jdk-25* /usr/lib/jvm/*25*; do
		if [[ -x "${candidate}/bin/javac" ]]; then
			javac_bin="${candidate}/bin/javac"
			break
		fi
	done
fi
if [[ -z "${javac_bin}" ]] && command -v javac >/dev/null 2>&1; then
	javac_bin="$(command -v javac)"
fi

install_hint="install a JDK 25 into ~/.jdks (Adoptium Temurin) or run: sudo apt install -y openjdk-25-jdk"

if [[ -z "${javac_bin}" ]]; then
	report "javac" "MISSING — ${install_hint}"
	status=1
else
	javac_version="$("${javac_bin}" -version 2>&1 | awk '{print $2}')"
	if [[ "${javac_version}" == 25.* ]]; then
		report "javac" "${javac_version} (${javac_bin})"
	else
		report "javac" "${javac_version} — need 25. ${install_hint}"
		status=1
	fi
fi

report "JAVA_HOME" "${JAVA_HOME:-<unset, falling back to PATH>}"

for tool in claude gh uv node pnpm cargo; do
	if command -v "${tool}" >/dev/null 2>&1; then
		report "${tool}" "$("${tool}" --version 2>&1 | head -1)"
	else
		report "${tool}" "not installed"
	fi
done

echo
echo "Bundle (make bundle, make bundle-smoke; not needed by make verify)"

for tool in curl sha256sum tar; do
	if command -v "${tool}" >/dev/null 2>&1; then
		report "${tool}" "found"
	else
		report "${tool}" "not installed — make bundle downloads and checks a Temurin with it"
	fi
done

if command -v xvfb-run >/dev/null 2>&1; then
	report "xvfb-run" "found"
else
	report "xvfb-run" "not installed — make bundle-smoke fails its relay check; sudo apt install xvfb or sudo pacman -S xorg-server-xvfb"
fi

if command -v dpkg-deb >/dev/null 2>&1 || { command -v ar >/dev/null 2>&1 && command -v bsdtar >/dev/null 2>&1; }; then
	report "deb" "can be unpacked (dpkg-deb, or ar and bsdtar)"
else
	report "deb" "cannot be unpacked — install dpkg, or ar and bsdtar, for make bundle-smoke"
fi

# libfuse2 is only needed to run an AppImage; the smoke test extracts it instead.
if [[ -n "$(ldconfig -p 2>/dev/null | grep 'libfuse.so.2')" ]]; then
	report "fuse2" "libfuse.so.2 found"
else
	report "fuse2" "libfuse.so.2 not found — an AppImage will not start without it (sudo apt install libfuse2)"
fi

if command -v pkg-config >/dev/null 2>&1 && pkg-config --exists webkit2gtk-4.1 gtk+-3.0; then
	report "tauri" "webkit2gtk-4.1 and gtk+-3.0 development files found"
else
	report "tauri" "webkit2gtk-4.1 or gtk+-3.0 development files not found — see the Tauri Linux prerequisites"
fi

echo
echo "Project"

# The brief in CLAUDE.md is the only thing telling an agent what it is building. Left
# unwritten it is worse than absent: four paragraphs of prompts are read on every request
# and say nothing. Deliberately a warning — a fresh project is meant to pass everything.
if [[ -f CLAUDE.md ]] && grep -q "REPLACE-ME" CLAUDE.md; then
	report "brief" "CLAUDE.md still has the placeholder — agents know how to build this, not what it is"
else
	report "brief" "written"
fi

exit "${status}"
