# 86. The desktop window reaches the clipboard through Tauri's plugin

Date: 2026-11-05

## Status

Accepted. Builds on ADR 0058 (the pane's copy and paste chords) and ADR 0057 (the dumb relay).

## Context

The pane's `ctrl+shift+c`, `ctrl+shift+v` and `shift+insert` went through `navigator.clipboard`. Their tests passed on a
clipboard in memory, but in the packaged window under Hyprland a paste read nothing and a copy wrote nothing: wry turns on
WebKitGTK's `javascript-can-access-clipboard` only when a window asks for it, which Tauri's window config cannot, so the page
was refused and the emulator dropped the refusal without a word.

## Decision

- **The clipboard is `tauri-plugin-clipboard-manager`**, Tauri's own plugin, behind the `Clipboard` interface the emulator
  already takes. `window/tauri.ts`'s `TauriClipboard` calls its `readText` and `writeText`; the capability grants
  `clipboard-manager:allow-read-text` and `allow-write-text` and nothing more.
- Text only: no image, no HTML, no primary selection.

## Consequences

- The relay stays dumb: the plugin is Tauri's, not a command of ours, and the sidecar never sees the clipboard.
- A clipboard the plugin refuses is still dropped without a word, as the emulator's `deferred:` marker says.
- Checked in the packaged window under Hyprland: three lines pasted after a reload arrive bracketed, and a `shift`+drag
  selection copies to the system clipboard.
