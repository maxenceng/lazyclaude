# 49. A hosted pty carries bytes, and each frontend emulates its own

Date: 2026-10-27

## Status

Accepted; amended 2026-10-27 with the repaint's checked facts, and 2026-10-29 with the emulator's roles. Supersedes ADR 0026's host port, encoding boundary and
library rule; ADR 0026's aggregate, chords and lifetime stand. Builds on ADR 0048.

## Context

ADR 0026 gave the one host adapter both the pty (pty4j) and the terminal emulator (jediterm): `HostPort` hands out a
`TerminalScreen`, and turns published keys, pastes and mouse events into bytes using the emulator's modes. The desktop
evolution adds a second kind of frontend, a Tauri window that emulates in xterm.js. xterm.js already encodes input,
answers `claude`'s terminal queries and draws its own screen, and LAZY-36 showed raw pty bytes over the sidecar are
enough for it. Nothing in `session`'s domain reads the screen: states come from the registry record, and only the shell
draws it.

## Decision

- **One frontend drives a session.** A hosted `claude` is driven only by the frontend whose backend started it, never
  by two viewers at once, since ADR 0048 has no attach and no second client.
- **Emulation belongs to the frontend.** The TUI's adapter in `session.infrastructure.primary` owns jediterm, the input
  encoding (keys, pastes, mouse by the emulator's modes) and the replies to `claude`'s terminal queries; on the desktop
  xterm.js does the same. `ArchitectureTest` keeps `com.jediterm..` in `session.infrastructure.primary` and `com.pty4j..`
  in `session.infrastructure.secondary`. Rejected: the backend emulating as well, a headless shadow beside xterm.js whose
  query replies would have to be silenced; and the backend sending a cell grid the desktop draws, which gives up xterm.js
  and contradicts what LAZY-36 validated.
- **`HostPort` is a pty only:** spawn with an output and an exit listener, directory check, write, resize, end. Output
  is pushed per read on the host's reader thread, and both directions are *pty bytes* (`PtyBytes`), already encoded by
  the frontend. `screenOf` and the screen-changed signal leave the port for the TUI adapter, and `TerminalKey`,
  `PastedText`, `TerminalMouse` and `TerminalScreen` move from `published` to `infrastructure.primary`, since only the
  shell reads them, as `EmulatorKeyPayload`, `EmulatorPastePayload`, `EmulatorMousePayload` and `EmulatorScreenView`.
- **The TUI reaches its emulators through ADR 0011/0012's roles.** `HostedEmulatorQuery` reads a session's screen and
  signals that one changed. `HostedEmulatorCommand` encodes and writes input, and resizes. Both share one
  `HostedEmulators`. *Emulator* is the word for one session's emulation; only `HostedTerminal`, jediterm's `Terminal`
  subclass, keeps the library's word. `ArchitectureTest` holds the shell to `*Query`, `*Command`, `*Payload` and `*View`
  in any `infrastructure.primary`. Rejected: one facade that reads and writes, recorded as an exception to the roles.
- **The backend's domain owns a session's lifetime.** A frontend asks to start, resume and end, and is told started,
  output and exited; `HostedSessionManager` keeps ADR 0026's resume refusal, directory check and exit warnings, and the
  TUI's quit and the sidecar's input closing end the sessions through the same path. Rejected: the desktop tracking and
  spawning sessions itself, which splits those rules across two languages.
- **A resize goes frontend first:** its emulator, then the pty, so what `claude` redraws on SIGWINCH lands in a buffer of
  the new size. The backend keeps no size but the one a launch starts with.
- **A frontend whose emulator starts empty gets a redraw.** On a re-attached pane the backend shrinks the pty by one row
  and, 50 ms later, restores it, so `claude` redraws its screen on SIGWINCH; the frontend asks for it with
  `session/repaint` (ADR 0057). Rejected: replaying a byte ring buffer, which can start
  inside an escape sequence; and a headless emulator in the backend for snapshots, half of the rejected shadow.

## Consequences

- The TUI does the encoding and emulation it did before, one layer out; the hosted adapter shrinks to `HostedPty`'s
  process half.
- What scrolled off a pane is lost on a redraw; only the screen `claude` repaints comes back.
- Checked on Claude Code 2.1.310 (LAZY-41): each real size change repaints a full frame (`\e[2J\e[H` and every row of
  its alternate screen), so an empty emulator ends up identical to one that saw the whole stream. Two resizes with no
  gap repaint nothing, 10 ms was always enough, and a same-size SIGWINCH repaints nothing either.
- The repaint re-sends the mouse modes, synchronized output and cursor visibility, but not the alternate screen,
  bracketed paste, focus reports, `?2031` or the kitty keyboard mode; a re-attached frontend restores those itself
  (ADR 0057).
- Two emulators answer terminal queries differently (device attributes, colour reports), so `claude` may see a slightly
  different terminal in each frontend.
- Whether the TUI reaches its sessions through the protocol stays open; with the emulator on the frontend's side either
  way works.
