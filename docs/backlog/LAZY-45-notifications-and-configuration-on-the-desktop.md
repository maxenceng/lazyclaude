---
id: LAZY-45
status: done
context: cross-context
type: decision
created: 2026-10-27
blocked_by: [LAZY-41]
---

# How do notifications, reserved chords and swallowed keys work in the desktop frontend?

## Question

How do notifications, reserved chords and swallowed keys work in the desktop frontend?

Whether the waiting notification is fired and closed by the sidecar over D-Bus as today, or by Tauri's notification
plugin (equal cost takes Tauri's way, ADR 0056), and what "watched" means for a window rather than a focused terminal;
whether `config.yml`'s reserved chords apply to the desktop's panes and how they are told apart from `claude`'s keys
in xterm.js; and which TUI keys the webview or xterm.js swallows, and what replaces each.

## Kind

grilling

## Resolution

Grilled 2026-10-27; recorded in
[ADR 0058](../adr/0058-the-sidecar-fires-notifications-and-the-desktop-frontend-owns-its-keys.md).

- **Notifications**: the sidecar fires them over `notify-send`/`gdbus`, unchanged, with a `// deferred: Linux only`
  comment naming Tauri's plugin as the route to another platform. Rejected: Tauri's plugin now, which needs a new push
  and costs more than it buys on Linux, so ADR 0056's equal-cost rule does not apply.
- **Watched**: the selected session pane's session while that pane and the window both have focus. Focus comes from
  Tauri's `isFocused()` and then `onFocusChanged`, with no "focused until told otherwise" default. The frontend calls
  `WatchedSessionCommand`'s method on every change.
- **Reserved chords**: the same `config.yml`, read by the sidecar and served through `ConfigurationQuery`'s method.
  They are matched on `KeyboardEvent.key` plus modifiers, by xterm.js's custom key handler in a pane and by one
  window-level handler outside a pane. The refusals made for TamboUI stay.
- **Fixed chords and `ctrl+z`**: as in the TUI. `ctrl+z` is kept from `claude`, and `ctrl+c` goes to `claude` in a pane
  and quits outside one.
- **Terminal keys**: `ctrl+shift+c` copies, `ctrl+shift+v`/`shift+insert` paste bracketed through `term.paste()`,
  `shift`+drag selects, and plain `ctrl+v` reaches `claude`. No font zoom, and the webview's zoom hotkeys are disabled.
- **Swallowed keys**: every TUI key belongs to the app, and outside a pane the window-level handler calls
  `preventDefault` on each one it binds. Which keys a packaged build still lets through is found in DESKTOP-1 by going
  through the key table.
- Not built: raising the window when a notification is clicked.

Graduated into acceptance criteria on [DESKTOP-1](DESKTOP-1-the-desktop-frontend-shows-the-board.md) and
[DESKTOP-2](DESKTOP-2-hosted-sessions-in-the-desktop-frontend.md).
