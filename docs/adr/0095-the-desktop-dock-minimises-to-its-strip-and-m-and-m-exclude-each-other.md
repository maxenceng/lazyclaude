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
  one again. `Pane`'s title row has one action slot, used for `×` and for minimise and restore.

## Consequences

- `w` has no jump to the next waiting session in the desktop window yet; for now it restores a minimised dock and ends a zoom.
