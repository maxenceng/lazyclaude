# 46. A zoom ends when focus moves, and the status columns' cursor is the board's selection

Date: 2026-10-25

## Status

Accepted. Builds on ADR 0008.

## Context

Zoom fills the screen with the Board's status columns, the main view or the dock. Each of those hides the panels, so any key that
moves focus has to say what becomes of the zoom, and the status columns need a cursor that the main view can follow.

## Decision

- **The zoom is tied to the focused pane.** `focusOn` clears it whenever the target differs from the pane that has focus, so every
  key that moves focus (`h`/`l` outside the columns, `alt+h/j/k/l`, `enter` on a ticket, `N`, `p`, a session ending) leaves the zoom
  first without each listing it. A key that is a no-op for focus keeps it: `alt+j` in the dock, `alt+k` on the main view. `w`, `alt+w`
  and `alt+m` do not move focus by themselves and leave it explicitly.
- **The cursor of the status columns is the Board's selection**, not a second one. It moves through `BoardRows` onto a ticket by id,
  unfolding that ticket's group if the Board had folded it, so the main view and the Sessions panel follow with no extra state, and
  `z` or `esc` leave with the selection where the cursor is.
- **A zoom is a shell state with four shapes** (`Zoom`), not a context concern: `board` is unchanged.

## Consequences

- A zoomed dock is the same dock drawn larger, so the pty resize that `m` drives follows with no change to how sessions are hosted.
- Unfolding the group of a ticket the cursor lands on changes the Board's fold state as a side effect of zooming.
- Popups are placed over the areas last drawn, so `n` leaves the status columns before it opens the verb picker over the Sessions panel.
