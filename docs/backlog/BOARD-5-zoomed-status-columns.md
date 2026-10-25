---
id: BOARD-5
status: in-review
context: board
type: feature
tracker_id: 76
created: 2026-10-10
---

# Zoom (`z`) shows the board as status columns

## Acceptance criteria

- [x] `z` on the Board opens status columns between the status line and the key bar: `draft`,
      `todo`, `in-progress` and `in-review` share the width; `done` is a narrow column with only
      its header and count. The dock folds to a one-line strip of its tabs and markers.
- [x] A ticket in a column shows what its board row shows: marker, id, title cut with `…`, and
      the PR glyph when it fits.
- [x] An `unreadable (n)` column follows `done`, listing each file with its reason, only when
      there are any.
- [x] `j`/`k` move within a column; `h`/`l` move to the next non-empty column, keeping the row
      index (clamped), and skip the folded `done`. A column taller than the screen scrolls to
      keep the cursor visible and shows `↓ n more`. A narrow terminal shortens titles; the zoom
      is never refused.
- [x] The cursor is the selection and the main view follows it. `z` or `esc` leave the columns
      with the selection where the cursor is; `enter` leaves and focuses that ticket's sessions.
- [x] `z` on the main view zooms it between the status line and the key bar, the dock as its
      strip; `j`/`k` scroll and stay zoomed; `z` or `esc` leave.
- [x] `alt+z` zooms or unzooms whatever has focus, everywhere. In a session it zooms the dock:
      the hosted session fills the space and its pty is resized to it, then resized back when
      the zoom ends. `alt+h`/`alt+l` stepping through tabs stay zoomed.
- [x] A key that moves focus or shows the main view while zoomed — `h`/`l` outside the columns,
      `alt+h/j/k/l` leaving the pane, `w`, `alt+w`, `alt+m` — leaves the zoom first, then acts.
- [x] A board re-read while the columns are open redraws them; the cursor follows the ticket id,
      and a ticket that vanishes leaves the cursor on the row that took its place.
- [x] The last hosted session ending in a zoomed dock ends the zoom.
- [x] `z` on a board with no ticket rows still opens the columns, all empty.
- [x] While zoomed, the key bar lists the zoom's own keys.

## Model decision

Shell only — no new aggregate, no new context, and `board` unchanged: `BoardView` already gives
the status groups in lifecycle order and the unreadable files. `context: board` and the id stay,
since LAZY-10 charted it as BOARD-5 and the status columns are most of the work; the main view
and dock zooms ride along in the same ticket by choice.

- **Shell.** One zoom state with four shapes: none, the Board's status columns, the main view,
  the dock. The status columns need their own name — `Column` already means the screen's left
  and right halves.
- **Session.** Nothing new: a zoomed dock reuses the hosted session resize `m` already drives,
  so how sessions are hosted does not change.

The layout of ADR 0008 and LAZY-10 is unchanged. Rejected: a minimum column width under which
the zoom is refused (one more rule and one more warning, for titles that only get shorter);
`esc` restoring the selection from before the zoom (a second selection to keep); a focusable
dock strip while the columns are open (a second dock UI); 1–5 to jump to a column (collides
with 1–4 jumping to panels, and needs Shift on AZERTY).

## Design direction

```
 ! 1  ? 2                                                   ⚠ …
┌ draft (2) ──────┬ todo (3) ───────┬ in-progress (1) ┬ in-review (0) ┬done 23┐
│ BOARD-6 Title f…│ SESSION-9 Conf… │!LAZY-23 Clear … │               │       │
│ DOCS-1 Docs tr… │ …               │                 │               │       │
└─────────────────┴─────────────────┴─────────────────┴───────────────┴───────┘
 dock: [? ticket BOARD-6] [∼ unlinked]
 h/l column  j/k move  enter pick  z/esc back
```

A zoomed main view or dock fills the same space between the status line and the key bar; the
zoomed main view keeps the dock as its strip, a zoomed dock is alone. Keys: `z` toggles the
zoom of the focused Board or main view, `esc` leaves it, `alt+z` toggles from anywhere —
the only way inside a session.

## Glossary impact

- adds **Zoom** (`shell`): `z`, or `alt+z` from anywhere; on the Board, the status columns across
  the screen; on the main view or the dock, that pane filling the screen between the status line
  and the key bar.
- Nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-10](LAZY-10-screen-layout-and-keys.md).
