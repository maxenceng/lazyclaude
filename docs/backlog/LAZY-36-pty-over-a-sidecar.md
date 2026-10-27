---
id: LAZY-36
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-24]
---

# Can a hosted `claude` run smoothly through a JSON Lines sidecar into xterm.js in a Tauri window?

## Question

Can a hosted `claude` run smoothly through a JSON Lines sidecar into xterm.js in a Tauri window?

A throwaway: Java spawns `claude` on pty4j, frames its output as JSON Lines on stdout, a Tauri app writes it into xterm.js and sends keys and resizes back. Measure input latency, throughput on a large output, the cost of the byte encoding (base64 or other), resize, Ctrl+C, paste, mouse reporting, and what happens when either side dies.

## Kind

prototype

## Resolution

Prototyped 2026-10-27 on Linux (Hyprland, Wayland, NVIDIA RTX 3070), Claude Code 2.1.310, pty4j 0.14.0,
Tauri 2.12. The prototype is on the throwaway branch `prototype/LAZY-36-pty-over-a-sidecar`, under
`prototypes/LAZY-36-pty-sidecar/`: a single-file Java sidecar run from source, a Tauri app whose Rust only relays lines,
xterm.js in the webview.

**Yes.** Driven by hand, a hosted `claude` behaved as in a native terminal: typing felt instant, a multi-line paste
arrived as one paste, Ctrl+C interrupted it, a resize redrew it cleanly, clicks and the wheel worked inside it, and the
window stayed responsive while `cat` streamed 119 MB through bash. The live latency and throughput figures in the
window were judged fine, not recorded.

Measured headless (119 MB of `seq` output through `cat`):

| path | wall time |
|---|---|
| raw pty (`script`) | 3.34 s — ~36 MB/s, the pty is the ceiling |
| sidecar, base64 | 4.31 s, of which ~0.6 s JVM start from source; encoding 137 ms in all |
| sidecar, one JSON-string char per byte | 4.52 s; encoding 2.7 s, 49 % more bytes on the wire |

What the protocol ([LAZY-38](LAZY-38-sidecar-protocol.md)) takes from it:

- **pty bytes travel as base64** in a JSON string: cheapest and smallest of the two tried, and it keeps every line
  valid UTF-8 JSON.
- **pty reads are small** — 119 MB came as ~330 000 reads, ~360 bytes each — so the cost is the message count, not
  JSON. The relay hands the webview whatever lines are waiting as one message (a Tauri `Channel`, not an event per
  line); the sidecar itself sends one line per read.
- **stdin EOF is the shutdown**: the sidecar destroys the pty and exits. A SIGKILLed sidecar closes the pty master and
  `claude` exits on the hangup; closing the window left no `claude`. ADR 0048's lifetime needs no extra mechanism.
- **stdout carries only protocol**: pty4j's SLF4J writes warnings to stderr at startup, so stderr is the log channel.
- Resize as its own message (`cols`, `rows`) and keys, paste and mouse reports as plain input bytes were enough —
  xterm.js produces bracketed paste and SGR mouse sequences itself.

Risks it surfaced, for the desktop frontend and packaging:

- **WebKitGTK on Wayland with NVIDIA needs `WEBKIT_DISABLE_DMABUF_RENDERER=1`**, or the window renders nothing usable.
- **While the window was open, the kitty window could not be focused** under Hyprland; it came back once the window
  closed. Not diagnosed: seen only under `tauri dev`, never a packaged build.
- Only Linux was tried; the Rust relay ran as a debug build.
