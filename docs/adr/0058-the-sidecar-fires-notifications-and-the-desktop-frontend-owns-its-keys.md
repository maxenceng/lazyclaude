# 58. The sidecar fires notifications, and the desktop frontend takes the TUI's keys and its terminal's

Date: 2026-10-27

## Status

Accepted. Builds on ADR 0033 (the desktop notification), ADR 0050 (the sidecar protocol), ADR 0056 (Linux x86_64
only) and ADR 0057 (the desktop frontend).

## Context

The desktop frontend (ADR 0057) shows the TUI's screens with the TUI's keys, but it runs in a WebKitGTK webview over
xterm.js rather than in a terminal. Three things the TUI gets from its terminal have to be decided again there:
- who fires a desktop notification;
- what *watched* means when there is a window rather than a terminal reporting its focus;
- which keys reach lazyclaude, `claude` or the webview.

Tauri's notification plugin can show, replace and close a notification on Linux, and cannot close one on macOS or
Windows (LAZY-39). ADR 0056 sends the desktop frontend Tauri's way when the cost is equal. kitty gives the TUI's panes
copy, paste and selection, and xterm.js gives none of these by itself.

## Decision

- **The sidecar fires the desktop notification, over `notify-send` and `gdbus` as in the TUI.** `DesktopNotificationManager`
  and `NotifySendRepository` are unchanged. Rejected: firing it from the frontend through Tauri's plugin. That would take
  a new protocol push of show, replace and close, and would turn the sidecar's notification port into a protocol output,
  which buys nothing on Linux. The cost is not equal, so ADR 0056's preference does not apply. The adapter carries a
  `// deferred: Linux only` comment naming the plugin as the route for another platform.
- **On the desktop, the watched session is the selected session pane's session while that pane and the window both
  have focus.** Window focus comes from Tauri's `isFocused()` at start, then from `onFocusChanged`. A window always
  knows its focus, so the TUI's "focused until a focus-out" default does not apply. The frontend calls
  `WatchedSessionCommand`'s method on every change.
- **The reserved chords come from the same `config.yml`, read by the sidecar.** The frontend takes them from
  `ConfigurationQuery`'s method and never reads the file. In a pane, xterm.js's `attachCustomKeyEventHandler` refuses
  a reserved chord. Outside a pane, one window-level `keydown` handler takes it. Both match `KeyboardEvent.key` and the
  modifiers, as the TUI matches the character. The refusals made for chords TamboUI cannot deliver stay: one file serves
  both frontends, so it refuses what either one cannot read.
- **Fixed chords and `ctrl+z` behave as in the TUI.** `ctrl+z` is kept from `claude` in a pane, since nothing could
  resume a suspended `claude` under a pty. `ctrl+c` goes to `claude` in a pane and quits outside one. `ctrl+d` and
  `ctrl+u` page outside a pane.
- **The frontend takes over the terminal's own keys.** In a pane:
  - `ctrl+shift+c` copies xterm.js's selection;
  - `ctrl+shift+v` and `shift+insert` paste text through `term.paste()`, so it arrives bracketed;
  - `shift`+drag selects past `claude`'s mouse reporting;
  - a plain `ctrl+v` still reaches `claude` as `^V`, for its image paste.

  Rejected: font zoom. The webview's zoom hotkeys are disabled, so neither `ctrl` nor the wheel zooms the page.
- **Every key in the TUI's key table belongs to the app.** Outside a pane, the window-level handler calls
  `preventDefault` on each key it binds. Inside one, xterm.js already cancels what it sends. The keys a packaged build
  still lets through are found by going through the key table in that build, not listed here.

## Consequences

- One `config.yml` serves both frontends. A chord the desktop could deliver but TamboUI cannot stays refused.
- On another platform, the desktop notification has to move to Tauri's plugin, and closing it there is blocked
  without native code (LAZY-39).
- A desktop notification does nothing when clicked. Raising the window on a click is not built.
- If Hyprland's focus oddity seen in LAZY-36 leaves the window looking focused, a waiting session in the selected pane
  is never notified. DESKTOP-2 checks for this.
