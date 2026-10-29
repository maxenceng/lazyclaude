# 51. The mouse moves focus and selection, and a second click is `enter`

Date: 2026-10-27

## Status

Accepted. Extends ADR 0008; amends ADR 0026 (when a hosted pane receives the mouse).

## Context

The desktop evolution's second phase gives the TUI the mouse for every primary navigation and selection workflow, keys
unchanged (ADR 0008). TamboUI 0.5.0 reports SGR press, release, drag, move and wheel, but routes only a left press, a
move and the vertical wheel to elements, outermost first, and never resolves a click to a row: a click focuses a panel
and selects nothing. It has no click count, and it does not decode X10, so on a terminal that ignores mode 1006 a click
types its coordinate bytes, and column 81 types `q`. A hosted `claude` asks for 1000/1002/1003 and acts on its own
clicks, menu rows and permission prompts included.

## Decision

- **The mouse only focuses, selects, switches dock tabs, scrolls and selects a diff range.** Every other action stays
  on its key. Right and middle clicks, the key bar, the status line and links do nothing; the outer terminal's Shift
  bypass keeps selection and link-clicks across the whole screen.
- **A second click is `enter`.** A press on the row that was already selected, in the pane that already had focus,
  does what `enter` does there: fold, pick, resume, focus a tab, open the picker, go to a line. Rejected: a timed
  double-click, which TamboUI cannot count and which a slow pair misses; and no click action at all, which leaves
  `enter` the one thing the mouse cannot reach.
- **A click on an unfocused hosted pane only focuses it.** Once the pane has focus, press, drag, release and wheel go
  to `claude` unchanged; the wheel goes through even on the notch that focuses. A focusing click never answers a
  permission prompt.
- **A drag keeps the pane it started in.** Its release is clamped to that pane's edge, so neither `claude` nor the diff
  loses it, and pointer motion reaches a hosted pane only while it has focus and its session asked for 1003; every other
  move is dropped before a render.
- **The wheel acts on the pane under the pointer and focuses it.** In a list it moves the selection a row a notch, so
  the selected row stays visible; in the main view and the diff it scrolls three lines.
- **Popups are modal.** A click outside the verb picker or a prompt does nothing, and never reaches what is under it.
- **Clicks resolve against a per-frame hit map.** Each element records its rectangle, first visible row and tab offsets
  as it renders; the shell's global mouse handler reads the last frame's map on the render thread. Rejected: recomputing
  the layout in the handler, which writes the layout twice.
- **Only the verb picker has rows to click.** The quit, removal, respond and submit prompts and the comment box have none, so
  every mouse event is taken and does nothing; a click on the focused pane's `claude` that only focused it has its drags and
  release dropped, since the session never saw the press. A wheel over the status columns moves the cursor a ticket.
- **`mouse: false` in `config.yml` turns mouse capture off**, and it is off by default when `TERM` is `linux`. lazyclaude
  writes no X10 decoder; the real fix is TamboUI's (#517).

## Consequences

- Nothing the mouse does cannot be undone by a key, except a second click, which is exactly an `enter`.
- A hosted session has no hover while it is unfocused, and a click that focuses it is lost to `claude`.
- Every pane the mouse reaches writes to the hit map, so a new pane with no entry is inert to the mouse until it adds
  one.
- deferred: a review's Files, Comments and diff stay on their keys, the mouse reaching none of them — closes when a ticket gives
  the review the mouse (the diff range ADR 0051 names).
- deferred: a click lands on the row index the last frame drew, so rows that moved between that frame and the click select their
  neighbour — closes when a mis-click on a board that re-read under the pointer is seen.
- deferred: a terminal answering in X10 still turns clicks into keys unless `mouse: false` is set — closes when
  TamboUI decodes X10 (#517).
- The desktop frontend takes its mouse from the browser; this ADR is the TUI's.
