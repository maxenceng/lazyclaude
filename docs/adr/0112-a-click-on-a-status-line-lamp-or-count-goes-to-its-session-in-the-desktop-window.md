# 112. A click on a status line lamp or count goes to its session in the desktop window

Date: 2026-11-14

## Status

Accepted. Amends ADR 0051 for the desktop window only: its rule that the status line does nothing to a click stands in the TUI.

## Context

The desktop window's status line shows a tile per live session and a count per marker. A developer who sees "1 your turn"
has to find that session's tab by eye. The window already has the operation that goes to a session, a click on its dock
tab, and the window has the data to reach it: a hosted session lists the live session its launch now runs, which after a
`/clear` is not the launch id its tab is keyed by.

## Decision

- **A click on a tile of a session the window hosts goes to its dock tab**, as a click on the tab does: the tab is
  selected and its pane focused. The tab is found through the live session the launch runs, not the launch id alone, so a
  `/clear`-replaced session is reached. The tile of a live session the window does not host is not a button.
- **A click on a count goes to the next hosted session of its colour after the selected tab, in dock tab order,
  wrapping**; from a tab of another colour, to the first one after it; with one of that colour, always to it. A count
  with no hosted session of its colour is a disabled button.
- **The move goes through the operation the dock tab calls.** `StatusLine` takes the clicks from `App.tsx`, since the
  design system cannot import `session`; the rule that picks the tab lives in `frontend/src/session/`.
- **The tiles and counts look like the dock tabs**: no hover change, default cursor, the blue focus edge from the keyboard.
  Lamp colours still mean session state only (ADR 0079). A disabled count dims its number and keeps its colour.
- The zoom key and its reserved chord still only leave the zoom.

## Consequences

- A count is a button even at zero, so the status line's tab stops grow by three; a disabled one is skipped.
- Cycling follows the dock's tab order, not the strength order the tiles are drawn in, so a click and the eye can disagree
  about "next".
- The TUI's status line stays inert; a click there is still ignored.
