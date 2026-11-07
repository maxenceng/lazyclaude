#!/usr/bin/env bash
# Smoke-tests a bundle's own runtime, jar and natives with a stub `claude`, then the relay under
# Xvfb (ADR 0059). With no arguments it tests the deb and the AppImage that `make bundle` built:
# an installed deb if there is one, else the deb unpacked; the AppImage extracted.
# With `<label> <resource-dir> <relay>` it tests that one bundle root, which is how a broken copy is tried.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${ROOT}/target/bundle/out"
PATIENCE=30
ALIVE_AFTER=5

failures=0
pass() { printf '  ok      %s\n' "$1"; }
fail() { printf '  FAILED  %s\n' "$1"; failures=$((failures + 1)); }

scratch="$(mktemp -d)"
pids=()
cleanup() {
	local pid
	for pid in "${pids[@]}"; do kill -KILL -- "-${pid}" 2>/dev/null; kill -KILL "${pid}" 2>/dev/null; done
	pkill -KILL -f "${scratch}/" 2>/dev/null
	rm -rf "${scratch}"
}
trap cleanup EXIT

# Polls a condition for up to PATIENCE seconds.
await() {
	local waited=0
	until "$@"; do
		sleep 0.2
		waited=$((waited + 1))
		[[ ${waited} -lt $((PATIENCE * 5)) ]] || return 1
	done
}

pty4j_dirs() { find /tmp -maxdepth 1 -name 'pty4j-*' 2>/dev/null | sort; }

# The stub echoes what it is typed and waits. With STUB_LEAKS it also leaves a copy of itself
# behind in its own session, which is how a stub the sidecar cannot reach is simulated.
write_stub() {
	local bin="$1"
	mkdir -p "${bin}"
	cat > "${bin}/claude" <<'STUB'
#!/bin/bash
if [ "$1" = "--linger" ]; then
	while :; do sleep 1; done
fi
echo $$ > "$STUB_PID_FILE"
if [ -n "${STUB_LEAKS:-}" ]; then
	setsid -f "$0" --linger </dev/null >/dev/null 2>&1
fi
echo stub-ready
while IFS= read -r line; do echo "typed: $line"; done
STUB
	printf '#!/bin/sh\necho "gh: not logged in" >&2\nexit 1\n' > "${bin}/gh"
	chmod +x "${bin}/claude" "${bin}/gh"
}

printed_by_session() {
	grep -a '"method":"pty/output"' "$1" | sed -E 's/.*"data":"([^"]*)".*/\1/' | while read -r chunk; do printf '%s' "${chunk}" | base64 -d 2>/dev/null; done
}

sidecar_checks() {
	local label="$1" res="$2" work="$3"
	local bin="${work}/bin" home="${work}/home" project="${work}/project"
	mkdir -p "${home}" "${project}"
	write_stub "${bin}"
	git init -q "${project}" || { fail "${label}: git init for the fixture project"; return; }
	local stub_pid="${work}/stub.pid"
	local before
	before="$(pty4j_dirs)"

	mkfifo "${work}/in"
	(
		cd "${project}" || exit 1
		export HOME="${home}" PATH="${bin}:${PATH}" STUB_PID_FILE="${stub_pid}"
		exec "${res}/runtime/bin/java" "-Dpty4j.preferred.native.folder=${res}/natives" "-Djna.boot.library.path=${res}/natives" \
			-Djna.nounpack=true --enable-native-access=ALL-UNNAMED -jar "${res}/lazyclaude.jar" --sidecar \
			< "${work}/in" > "${work}/out" 2> "${work}/err"
	) &
	local sidecar=$!
	pids+=("${sidecar}")
	exec 3> "${work}/in"

	has_line() { [[ -s "${work}/out" ]]; }
	if await has_line && [[ "$(head -1 "${work}/out")" == *'"method":"ready"'* ]]; then
		pass "${label}: ready comes first"
	else
		fail "${label}: ready comes first (first line: $(head -1 "${work}/out" 2>/dev/null | cut -c1-120); stderr: $(head -3 "${work}/err" 2>/dev/null | tr '\n' ' '))"
	fi

	printf '{"jsonrpc":"2.0","id":1,"method":"session/startUnlinked","params":{"root":"%s","size":{"width":100,"height":30}}}\n' "${project}" >&3
	stub_spoke() { printed_by_session "${work}/out" | grep -aq stub-ready; }
	if await stub_spoke; then
		pass "${label}: a hosted session starts on the stub claude from PATH"
	else
		fail "${label}: a hosted session starts on the stub claude from PATH (stderr: $(head -3 "${work}/err" 2>/dev/null | tr '\n' ' '))"
	fi

	exec 3>&-
	sidecar_gone() { ! kill -0 "${sidecar}" 2>/dev/null; }
	if await sidecar_gone; then
		pass "${label}: the sidecar exits when stdin closes"
	else
		fail "${label}: the sidecar exits when stdin closes (still running after ${PATIENCE}s)"
		kill -KILL "${sidecar}" 2>/dev/null
	fi

	stub_left() { [[ -f "${stub_pid}" ]] && kill -0 "$(cat "${stub_pid}")" 2>/dev/null; }
	stray() { pgrep -f "${bin}/claude" >/dev/null; }
	not_left() { ! stub_left && ! stray; }
	if [[ -f "${stub_pid}" ]] && await not_left; then
		pass "${label}: no stub claude is left"
	else
		fail "${label}: no stub claude is left ($([[ -f "${stub_pid}" ]] || echo 'the stub never started; ')$(pgrep -fa "${bin}/claude" | head -2 | tr '\n' ' '))"
		pkill -KILL -f "${bin}/claude" 2>/dev/null
	fi

	local after
	after="$(pty4j_dirs)"
	if [[ "${before}" == "${after}" ]]; then
		pass "${label}: no /tmp/pty4j-* is left"
	else
		fail "${label}: no /tmp/pty4j-* is left (new: $(comm -13 <(echo "${before}") <(echo "${after}") | tr '\n' ' '))"
	fi
}

relay_check() {
	local label="$1" res="$2" relay="$3" work="$4"
	if ! command -v xvfb-run >/dev/null 2>&1; then
		fail "${label}: the relay spawns the sidecar under xvfb-run (xvfb-run is not installed: sudo apt-get install xvfb, or sudo pacman -S xorg-server-xvfb)"
		return
	fi
	local project="${work}/relay-project"
	mkdir -p "${project}"
	git init -q "${project}"
	# Never the real display: Wayland is unset and xvfb-run brings its own X server.
	setsid env -u WAYLAND_DISPLAY -u DISPLAY HOME="${work}/home" timeout 60 xvfb-run -a "${relay}" "${project}" > "${work}/relay.out" 2>&1 &
	local group=$!
	pids+=("${group}")

	sidecar_of_relay() {
		local java
		for java in $(pgrep -f "${res}/lazyclaude.jar"); do
			local parent
			parent="$(ps -o ppid= -p "${java}" | tr -d ' ')"
			if [[ "$(readlink "/proc/${parent}/exe" 2>/dev/null)" == "$(readlink -f "${relay}")" ]]; then
				echo "${java} ${parent}"
				return 0
			fi
		done
		return 1
	}
	local pair
	spawned() { pair="$(sidecar_of_relay)"; }
	if ! await spawned; then
		fail "${label}: the relay spawns the sidecar under xvfb-run ($(tail -3 "${work}/relay.out" | tr '\n' ' '))"
		kill -KILL -- "-${group}" 2>/dev/null
		return
	fi
	sleep "${ALIVE_AFTER}"
	local java="${pair% *}" parent="${pair#* }"
	if kill -0 "${java}" 2>/dev/null && kill -0 "${parent}" 2>/dev/null; then
		pass "${label}: the relay and its sidecar are both alive after ${ALIVE_AFTER}s under xvfb-run"
	else
		fail "${label}: the relay and its sidecar are both alive after ${ALIVE_AFTER}s ($(tail -3 "${work}/relay.out" | tr '\n' ' '))"
	fi
	kill -TERM -- "-${group}" 2>/dev/null
	sleep 1
	kill -KILL -- "-${group}" 2>/dev/null
}

smoke() {
	local label="$1" res="$2" relay="$3"
	echo "${label}: ${res}"
	for part in runtime/bin/java lazyclaude.jar; do
		[[ -e "${res}/${part}" ]] || { fail "${label}: ${res}/${part} is missing from the bundle"; return; }
	done
	[[ -x "${relay}" ]] || { fail "${label}: the relay ${relay} is not an executable"; return; }
	local work="${scratch}/${label}"
	mkdir -p "${work}"
	sidecar_checks "${label}" "${res}" "${work}"
	relay_check "${label}" "${res}" "${relay}" "${work}"
}

extract_deb() {
	local deb="$1" into="$2"
	if command -v dpkg-deb >/dev/null 2>&1; then
		dpkg-deb -x "${deb}" "${into}"
	else
		(cd "${into}" && ar x "${deb}" && bsdtar -xf data.tar.*)
	fi
}

if [[ $# -eq 3 ]]; then
	smoke "$1" "$2" "$3"
elif [[ $# -eq 0 ]]; then
	deb=("${OUT}"/*.deb)
	appimage=("${OUT}"/*.AppImage)
	[[ -f "${deb[0]}" && -f "${appimage[0]}" ]] || { echo "bundle-smoke: no deb and AppImage in ${OUT}: run make bundle" >&2; exit 1; }

	if dpkg -s lazyclaude >/dev/null 2>&1; then
		smoke deb /usr/lib/lazyclaude /usr/bin/lazyclaude
	else
		echo "deb: not installed, unpacking it instead of installing it"
		mkdir -p "${scratch}/deb-root"
		extract_deb "${deb[0]}" "${scratch}/deb-root"
		smoke deb "${scratch}/deb-root/usr/lib/lazyclaude" "${scratch}/deb-root/usr/bin/lazyclaude"
	fi

	mkdir -p "${scratch}/appimage-root"
	(cd "${scratch}/appimage-root" && "${appimage[0]}" --appimage-extract >/dev/null) || fail "appimage: --appimage-extract"
	smoke appimage "${scratch}/appimage-root/squashfs-root/usr/lib/lazyclaude" "${scratch}/appimage-root/squashfs-root/usr/bin/lazyclaude"
else
	echo "usage: bundle-smoke.sh [<label> <resource-dir> <relay>]" >&2
	exit 2
fi

echo
if [[ ${failures} -ne 0 ]]; then
	echo "bundle-smoke: ${failures} check(s) failed"
	exit 1
fi
echo "bundle-smoke: every check passed"
