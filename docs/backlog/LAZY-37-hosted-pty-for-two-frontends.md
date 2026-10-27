---
id: LAZY-37
status: done
context: cross-context
type: decision
created: 2026-10-26
blocked_by: [LAZY-26, LAZY-36]
---

# How does `session` expose a hosted pty to two frontends?

## Question

How does `session` expose a hosted pty to two frontends?

Today the one host adapter holds pty4j and jediterm (ADR 0026). Where terminal emulation lives once the TUI emulates in jediterm and the desktop in xterm.js; what the `HostPort` and `HostedSession` publish (a raw byte stream, a grid, both); and who owns resize and the session's lifetime.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in [ADR 0049](../adr/0049-a-hosted-pty-carries-bytes-and-each-frontend-emulates-its-own.md).

- **One frontend per session**: a `claude` is driven only by the frontend whose backend started it; no fan-out.
- **Emulation belongs to the frontend**: the TUI's adapter in `session.infrastructure.primary` owns jediterm, the input
  encoding and the replies to terminal queries; the desktop uses xterm.js. Rejected: a backend shadow emulator, and a
  cell grid sent to the desktop.
- **`HostPort` is a pty only**: spawn (output and exit listeners), exists, write, resize, end; `PtyBytes` both ways,
  output pushed per read. `screenOf`/`onScreenChange` move to the TUI adapter; `TerminalKey`, `PastedText`,
  `TerminalMouse`, `TerminalScreen` move from `published` to `infrastructure.primary`.
- **Lifetime stays in the backend's domain**: frontends ask and are told; `HostedSessionManager` keeps the resume
  refusal, directory check and exit warnings.
- **Resize** goes emulator first, then pty; the backend keeps no size.
- **Repaint** of an empty emulator: shrink the pty a row and restore it, so `claude` redraws on SIGWINCH; to be
  checked before the desktop phase, with "no reload in the packaged build" as fallback.

Graduated into [SESSION-13](SESSION-13-the-pty-port-carries-bytes.md).
