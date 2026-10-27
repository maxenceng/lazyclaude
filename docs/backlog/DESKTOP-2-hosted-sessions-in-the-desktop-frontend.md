---
id: DESKTOP-2
status: todo
context: desktop
type: feature
tracker_id: 113
created: 2026-10-27
---

# Hosted sessions run in the desktop frontend's panes

## Acceptance criteria

- [ ] Starting, resuming and starting in a ticket's worktree work from the Board as in the TUI, each refusal shown as
      the TUI shows it.
- [ ] A hosted session's pane is an xterm.js terminal fed by `pty/output`; its keys, pastes and mouse reports go out as
      `pty/input` bytes, xterm.js encoding them.
- [ ] A resize fits xterm.js first, then sends `pty/resize` (ADR 0049).
- [ ] `session/exited` shows in the pane, and `session/watch`'s View drives the session list and states.
- [ ] Per `SessionId`, the page keeps in `sessionStorage` the private modes its emulator was set to (alternate screen,
      bracketed paste, focus reports, `?2031`, the kitty keyboard mode and any other it sees).
- [ ] After a reload, each pane writes its kept modes into its new emulator, then sends `session/repaint`; the pane then
      matches what it showed before, its pastes still bracketed.
- [ ] A packaged build swallows F5 and Ctrl+R and shows no context menu; the window offers Reload when the page stops
      answering.
- [ ] In a pane, xterm.js's custom key handler keeps every reserved chord and `ctrl+z` from `claude`, and they act as in
      the TUI. `ctrl+c` and plain `ctrl+v` reach `claude` (ADR 0058).
- [ ] `ctrl+shift+c` copies the pane's selection. `ctrl+shift+v` and `shift+insert` paste text bracketed through
      `term.paste()`. `shift`+drag selects while `claude` reports the mouse.
- [ ] The frontend calls `WatchedSessionCommand`'s method whenever the selected pane, its focus or the window's focus
      changes: Tauri's `isFocused()` at start, then `onFocusChanged`. A session waiting in the selected pane fires a
      desktop notification once the window loses focus, under Hyprland as well.
- [ ] Tests cover the mode keeping and restoring with xterm.js's parser driven by recorded bytes.

## Model decision

No backend change beyond SIDECAR-3's `session/repaint` (ADR 0049, ADR 0057).

## Design direction

The session pane as in the TUI. Its keys, copy and paste are decided in
[LAZY-45](LAZY-45-notifications-and-configuration-on-the-desktop.md) (ADR 0058).

## Glossary impact

None.

## Notes

Charted on the [LAZY-24](LAZY-24-desktop-evolution.md) map; decided in
[LAZY-41](LAZY-41-desktop-frontend-architecture.md), recorded in ADR 0057, the repaint's facts in ADR 0049. Blocked by
[DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md) and
[SIDECAR-3](SIDECAR-3-hosted-sessions-over-the-sidecar.md). LAZY-36 saw the kitty window lose focus under Hyprland
while the window was open in `tauri dev`; note whether it recurs.
