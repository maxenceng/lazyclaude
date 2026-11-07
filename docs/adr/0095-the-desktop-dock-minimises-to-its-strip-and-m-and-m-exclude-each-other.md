# 95. The desktop dock minimises to its strip, and `m` and `M` exclude each other

Date: 2026-11-07

## Status

Accepted. Amends ADR 0093 for the desktop window's dock strip; the TUI is unchanged.

## Context

The desktop window's dock could only be hidden by hiding the main view (`m` leaves the dock alone in the column) or
zoomed. A user who wanted the main view tall while sessions ran had no way to keep the sessions' tabs and lamps in sight.
ADR 0093 introduced a dock strip for the Board and Pull requests tabs; a second strip would have been a second thing to keep.

## Decision

- **The window's layout holds whether the dock is minimised**, beside `mainHidden` and the shown document. No context and
  no sidecar method changes.
- **A minimised dock reuses the dock strip** of ADR 0093, with a restore button; the main view takes the column's height.
- **`m` and `M` are exclusive, the last wins**, because a hidden main view and a minimised dock together would leave the
  column empty. `M` with the main view hidden shows it and folds the dock; `m` with the dock minimised unfolds it and
  hides the main view. Modelled as two flags the layout keeps from both being set, not as one three-valued enum, so the
  existing `mainHidden` rules stay as they are.
- **A session is never focused while hidden**: focusing the dock, whether by the focus-down chord, `w` or a session
  starting, restores it first. Minimising a focused dock moves focus to the ticket's sessions (the diff in a review).
- **`M` reaches a hosted session's pane like any plain key**; it minimises from every other panel.
- **The last session exiting** restores the flag, so the next session's dock opens unfolded.
- **A shown document closes** with its title row's button or `esc` on the Docs panel; the cursor moving or `enter` shows
  one again. `Pane`'s title row has one action slot, used for `×` and for the dock's minimise; the strip carries restore.
- **The ticket hides too**: while a dock is open, the ticket's title row carries `×`, which hides the main view as `m`
  does, so the dock fills the column. The key bar then offers `m ticket` in place of `M minimise`, which keeps it within
  80 columns and names the way back.
- **A pane's title row stays put and only its body scrolls**, so its button is never under WebKitGTK's overlay scrollbar
  nor scrolled away on a long ticket.
- **Only the focused pane shows its cursor row in the bright fill.** Every other pane keeps its cursor as a quiet card
  fill with a dim left edge, and the focused pane is marked by a lit title and a top accent rather than an edge all
  round. Two equally bright cursors read as two selections; `Pane` sets the cursor tokens, so no row knows its pane.
- **A click on a pane's button or a dock tab leaves the browser focus where it was**: the window's focus is its own
  `Layout`, and a button holding the DOM focus drew a ring nobody asked for.

## Consequences

- A row drawn outside any `Pane` keeps the bright cursor, the tokens' default.
- `w` has no jump to the next waiting session in the desktop window yet; for now it ends a zoom and focuses the dock, restoring it.
