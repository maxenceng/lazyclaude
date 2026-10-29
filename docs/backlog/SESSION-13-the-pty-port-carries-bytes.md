---
id: SESSION-13
status: in-review
context: session
type: chore
tracker_id: 96
created: 2026-10-27
---

# The pty port carries bytes, and the TUI emulates its own

## Acceptance criteria

- [x] `HostPort` is spawn (with an output and an exit listener), exists, write, resize and end; output reaches its
      listener as `PtyBytes` per read, on the host's reader thread, and input is written as `PtyBytes` unchanged.
- [x] No class in `session.domain`, `session.application` or `session.infrastructure.secondary` uses jediterm;
      `ArchitectureTest` keeps `com.jediterm..` in `session.infrastructure.primary` and `com.pty4j..` in
      `session.infrastructure.secondary`, each rule watched to fail on a class put on the wrong side.
- [x] The TUI's adapter in `session.infrastructure.primary` feeds each hosted session's output into its own jediterm
      emulator, encodes keys, pastes and mouse events by that emulator's modes, and writes its replies to `claude`'s
      terminal queries back to the pty.
- [x] `TerminalKey`, `PastedText`, `TerminalMouse` and `TerminalScreen` live in `session.infrastructure.primary`; the
      shell reads the screen and the screen-changed signal from the TUI adapter, not from `HostPort`.
- [x] A resize reaches the emulator before the pty.
- [x] Hosted sessions behave in the TUI as before: typing, bracketed paste, mouse in `claude`, arrows in application
      cursor mode, resize redraw, exit closing the tab.

## Model decision

No new aggregate (ADR 0049). `HostedSessionManager` keeps lifetime, resume refusal, directory check and exit warnings;
its screen pass-through goes. `HostedPty` keeps the process half; `HostedTerminal`, `ScreenDisplay`, `ScreenCopy`,
`TerminalReplies`, `KeyBytes`, `MouseBytes` and `PasteBytes` move to the TUI adapter.

## Design direction

n/a: no screen changes.

## Glossary impact

*Pty bytes* is already in; *Hosted session* unchanged.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map. Decided in
[LAZY-37](LAZY-37-hosted-pty-for-two-frontends.md), recorded in ADR 0049. Blocked by
[SESSION-11](SESSION-11-session-id-known-at-launch.md), which renames `HostedSessionId` to `SessionId` across the same
port. The SIGWINCH repaint is the desktop's need and lands with it, not here.
