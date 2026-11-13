# 105. The desktop window does not show unreadable tickets, and the Board tab previews beside its columns

Date: 2026-11-13

## Status

Accepted.

## Context

The Board tab stacked a preview of the cursor's ticket under the status columns, taking half the window's height, so the
columns that answer "where does each ticket stand?" were clipped. Every card looked alike apart from its column's styling,
which dimmed, dashed or flattened some columns' cards, and nothing on a card said a session was waiting on you. An
unreadable section sat beside the columns and an unreadable row under the Base tab's Board.

The glossary said an unreadable ticket is "never dropped silently", and PRODUCT.md's third principle that absence and
failure are shown with their reason. The user, refining the ticket, asked for unreadable tickets not to be shown in the
window at all: they are repaired on GitHub, through `/ticket`, whose `board` lists them with their reasons, and the TUI still
lists them too. A throwaway prototype (variant I, on variant D's visual system and variant H's facts line) was drawn and
chosen.

## Decision

- **The desktop window draws no unreadable ticket.** Neither the Board tab nor the Base tab's Board has an unreadable section
  or row. `board`'s View still carries them, and the TUI and `/ticket board` still list them with their reasons: only the
  desktop's drawing changes, so no context's model does. An unavailable board (the issues could not be read) is a failure
  of the read, not an unreadable ticket, and is still shown.
- **The preview sits beside the columns**, a full-height pane to the right, laid out as the Base tab's main view lays out a
  ticket: facts line, sessions, prose. With no ticket under the cursor it says `No ticket selected.`. It is always shown, so
  the preview's open and closed state, and the click where no card is that closed it, are gone: they existed to give the
  columns back the height the preview now never takes.
- **Every status keeps an equal-width column**, empty or not, with its count and `none`.
- **Every card looks the same whatever its column**: one fill, outline, ink and footer. A card carries a left edge in its
  type's colour, its title to three lines cut with an ellipsis, a footer of number and type, its open pull request's mark
  and CI, and the strongest waiting session's worded pill (as on the Base tab's Board row) with the card tinted in that
  lamp's colour. A ticket with no `type:` label has no edge and no type; a busy or ended session adds nothing. The
  type is written as well as coloured, so the edge is never the only sign. The fix colour was lightened a step so it reads
  at 4.5:1 on the your-turn tint.
- **The preview reuses Base's ticket pane**, so its facts line leaves out branch and worktree as ADR 0104's does, though the
  prototype drew them. A click on one of its session rows goes to the ticket on Base and runs the row's action, the one the
  row is labelled with.
- **`c` captures a draft on the Board tab too**, beside the title's `+`, so the key bar's new-draft key acts there; the
  prototype's `n` was not the bound chord.

## Consequences

- An unreadable ticket is learned of from `/ticket board` or the TUI, not from the window; PRODUCT.md's third principle
  carries that exception.
- The type edge is what the impeccable detector calls a side-tab accent; it is kept because the user picked it.
