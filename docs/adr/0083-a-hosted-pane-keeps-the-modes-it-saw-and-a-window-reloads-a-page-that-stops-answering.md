# 83. A hosted pane keeps the modes it saw, and the window reloads a page that stops answering

Date: 2026-11-05

## Status

Accepted. Builds on ADR 0049 (the repaint restores only part of a terminal's state), ADR 0057 (the page keeps the modes
a reload loses) and ADR 0058 (the watched session and the pane's keys), which it amends.

## Context

ADR 0057 decided that the page keeps, per `SessionId`, the private modes its emulator was set to, and that a packaged
build cannot reload. Building the hosted pane left several choices that someone could undo without knowing why: what
counts as a mode, when a reloaded pane asks for its repaint, how a window that cannot reload offers it, and what the
window's focus is before Tauri answers.

## Decision

- **Every `CSI ? h` and `CSI ? l` is kept, and the kitty keyboard flags.** The pane registers xterm.js parser handlers
  that record the last word on each private mode number and let xterm.js apply it as it would; a mode no one listed is
  kept too, as the ticket asked for "any other it sees". The kitty flags are kept as the stack `CSI > flags u`,
  `CSI < n u` and `CSI = flags ; how u` build, since xterm.js 6 ignores them and a restoration is only the bytes. A
  soft reset (`CSI ! p`) or a full one (`ESC c`) forgets all of it. A mode is restored as written, `h` or `l`, so a
  default-on mode `claude` turned off is turned off again.
- **The store is `sessionStorage`, one JSON entry per `SessionId`, `lazyclaude.modes.<id>`.** It survives a reload and
  not a quit, as ADR 0057 said. An entry that does not parse is none. A session that exits has its entry removed; a
  tab dropped from `session/watch`'s list does too.
- **A page with an entry for a hosted session it finds restores, fits, and only then asks.** The new emulator is
  written the kept modes, rebuilding its own record from those bytes rather than taking the stored one, which would
  push the kitty stack a second time on each reload. `session/repaint` waits for both the write's callback and the
  pane's first fit and `pty/resize`, so the repaint lands in an emulator already in bracketed paste and the alternate
  screen, and at the size `claude` redraws for: the pty keeps its size across a reload, so a fit sent after the
  repaint changes nothing and draws nothing again (ADR 0049). A tab not selected asks when it is first shown. There is
  no other signal of a reload, so the entry's presence is it.
- **The page tells the window it is alive; the window offers Reload.** The page calls a `page_alive` command once a
  second. The Rust side, in a packaged build only, checks every second, and a page silent for ten seconds (a page
  loading included) gets a menu of the window's own with one item, Reload, which calls the webview's `reload`. The menu
  is withdrawn when the page beats again. Rejected: a key, since a hung page takes none, and Tauri's dialog plugin,
  which is a dependency for one question. The relay still knows no sidecar method (ADR 0057).
- **The window's focus is unknown until Tauri answers, and unknown means not focused.** A wrong guess of "focused"
  would hold back a desktop notification for a session nobody is watching; a wrong guess of "not" notifies one that is
  watched. The page listens first and reads `isFocused()` second, and a change heard before the answer wins.
- **The pane's keys are classified apart from xterm.js.** `paneKeyOf` takes a press and whether it is a reserved chord
  (the composition side knows the chords; no context imports `keys/`), and says reserved, `ctrl+z`, copy, paste or
  the terminal's. The handler cancels the browser's own action for all but the last, and acts on the key down only.
  The window-level handler ignores a key whose target is inside an element marked `data-terminal`, since a
  `preventDefault` there would also cancel the `keypress` xterm.js types printable characters from.
- **Paste and copy go through the webview's clipboard.** `navigator.clipboard.readText()` into `term.paste()`, and
  `getSelection()` into `writeText()`. A clipboard the webview refuses is dropped without a word.

## Consequences

- A mode `claude` sets while no page is open is lost until `claude` sets it again; the repaint re-sends the mouse modes
  and synchronized output either way.
- A restored `?1049h` switches the new emulator to its alternate screen at once, before the repaint lands.
- The verb picker starts on the first verb of the ticket, not the one the TUI preselects, since the protocol carries
  no verb choice (a `deferred:` comment on `VerbPicker`).
- The heartbeat and the window's focus live in `window/`, on the composition side (ADR 0082), since only the
  composition side talks to Tauri's window.
- Whether WebKitGTK lets the page read the clipboard without a prompt, whether the Reload menu draws in a packaged
  build under Hyprland, and whether `isFocused()` is right there (ADR 0058's open point) were not run, only reasoned.
