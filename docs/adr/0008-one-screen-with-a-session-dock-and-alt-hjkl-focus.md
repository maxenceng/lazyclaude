# 8. One screen with a session dock, and `alt+hjkl` to move focus

Date: 2026-10-10

## Status

Accepted

## Context

lazyclaude shows the board, a ticket's sessions, the docs and other projects' waiting
sessions, and hosts sessions as the real `claude` on a pty (ADR 0007). A focused session pane
receives every key, so leaving it needs a key `claude` never uses. The user types on AZERTY,
where `[`, `]` and `ctrl+]` need AltGr and the digit row needs Shift, and wants lazygit's
vim-style keys.

A throwaway prototype compared three layouts: lazygit's side panels with a main view, kanban
columns with a stack of full screens, and an IDE workbench with a persistent dock. The user
kept the panels, the columns as a zoom, and the dock.

## Decision

One screen. Four panels on the left (Board, Sessions, Docs, Elsewhere). On the right, a main
view that follows the selection, over a dock that holds every hosted session as a tab. The
main view can be hidden so the dock fills the right column. Zoom turns the Board into status
columns, or the main view or dock into the full screen.

Outside a session pane, keys follow lazygit: `j`/`k`, `h`/`l`, `enter`, `?`, `q`. From
anywhere, a focused session included, `alt+h/j/k/l` move focus between panels, main view and
dock, with `alt+h`/`alt+l` also stepping through dock tabs. `alt+m`, `alt+z` and `alt+w`
hide the main view, zoom and go to the next waiting session. Nothing the user needs takes
`[`, `]` or a `ctrl+]` chord.

## Consequences

- **Hosted sessions never leave the screen.** The board stays visible while `claude` works,
  at the cost of width for the session. Hiding the main view or zooming the dock wins it back.
- **lazyclaude reserves `alt+h/j/k/l/m/z/w` inside every hosted session.** Claude Code 2.1.294
  binds none of them on Linux. A later Claude Code default on one of them would be shadowed,
  so the key set belongs in the future configuration, and a contract check against Claude
  Code's documented bindings is worth having.
- **The way out of a pane is a modified key, not the chord ADR 0007 anticipated.**
- **`alt` must reach lazyclaude.** kitty sends `alt+letter` as ESC-prefixed by default; the
  input layer has to tell `alt+h` apart from Escape followed by `h`.
- **Tabs inside panels are avoided.** Unlinked sessions are a section of the Sessions panel.
  A future panel that wants tabs has to find an AZERTY-friendly key first.
