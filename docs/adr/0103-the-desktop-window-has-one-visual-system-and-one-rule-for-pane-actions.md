# 103. The desktop window has one visual system and one rule for pane actions

Date: 2026-11-13

## Status

Accepted.

## Context

The desktop window had grown screen by screen: seams one pixel wide between panes, a focus told by a thin blue top edge, a
cross that closed a document, hid the ticket and quit the app alike, counts that were bare numbers, a `new session` row at
the bottom of a scrolling list, and dim marks (the unlit lamp, the terminal's bright black, an ended session) that missed
4.5:1. Four variants were drawn as throwaway prototypes and compared on screen; the user chose the look of one, the focus
of another and the pane actions of a third.

## Decision

- **Ground.** Panes sit on opaque grounds (`--pane`) over a darker window ground (`--ground`), separated by gutters
  (`--gutter`), set in Barlow. A seam inside a pane stays a hairline.
- **Focus.** The focused pane's title row is lit (`--lit` ground, `--lit-ink` ink) and the other panes recede to a dimmer ink.
  The selected row is solid only in the focused pane; in every other pane it stays, outlined, and is never cleared. Rows read
  it through `--cursor-ground`, `--cursor-ink`, `--cursor-border` and `--cursor-edge`, which `Pane` sets once.
- **Colour means something.** Status group headings take their status's colour, type tags (`feature`, `fix`, `chore`) their
  own, a your-turn session is tinted from the lamp tile token. Every colour is a token; every token pair that carries text or
  a mark is tested at 4.5:1 by reading `tokens.css`, not hardcoded hex.
- **Pane actions.** Close, hide, minimise, restore, zoom and add each have their own icon, and only close is a cross. They sit
  in one fixed slot at the right end of their pane's title row, with a tooltip and an accessible label that name the action and
  its key. Every action keeps its key; a key-only action is incomplete, and so is a button without its key.
- **Quit is not a pane's close.** The status line carries no cross: quit is an item of the `lazyclaude ▾` window menu at its
  far left, and goes through the quit confirmation `q` goes through.
- **Counts say what they count.** A window tab's count is a badge (`1 in progress`, `0 open`); a lamp count is worded
  (`0 permission`, `1 input`, `1 your turn`); the status line's session tiles keep a fixed two-row grid so numbers line up.
- **The status line reads left to right as window, then sessions.** The window menu, the window tabs as one group with the
  selected tab lifted, then the project; at the right the counts, coloured by their lamp while non-zero and dim at zero, a
  divider, and the session tiles in the prototype's size with a mono number. The key bar sets each key in bright mono
  before what it does in dim.
- **Closing a document hands focus to the ticket.** `Layout.closeDocument` no longer keeps focus where it was: it goes to the
  ticket the main view shows again, from the Docs panel or the dock. `esc` on the Docs panel with no document shown still
  goes up to the Board.
- **A click in a pane focuses it, and a shown document takes focus of its own.** A press anywhere in the ticket or the
  document, title row and body included, hands that pane focus and lights its title row; a press on a title-row action does
  not, since the action decides where focus goes. The document was the Docs panel's until now, with no focus of its own, so
  clicking it lit nothing: it now has one (`Focus.Document`), reached by a click, `l` from the Docs panel or going up from the
  dock, where `j`/`k` scroll it a line, `ctrl+d`/`ctrl+u` half a page, `h` goes back to the Docs panel keeping it shown, and `esc`
  closes it to the ticket. Focusing any other pane still closes it, as before.
- **New session is a bar.** It is pinned under the ticket, full width and out of the scrolling body, and its verb picker grows
  upward from it over the body; `esc` or a click outside closes it. The sessions list spans the main view while the body text
  keeps its reading measure.
- **Whole rows.** A pane body or a status column that scrolls rows is capped to the last row edge that fits, recomputed on
  scroll and resize, so no row is drawn half cut.
- **The key bar shows live keys only.** The Pull requests tab offers `j/k move` and `enter review` only while a pull
  request is listed.

## Consequences

- The look is set in `tokens.css` and the design-system primitives alone; no screen restyles anything.
- Whole rows are a JavaScript measure, not CSS, because rows differ in height and CSS has no "round the container to a row".
  The scroller's height shifts by under a row as it scrolls, and the strip left under the last whole row is blank.
- jsdom lays nothing out: the hook's arithmetic, the rules and the markup are tested, and how the panes look and whether any
  row is cut is confirmed on screen by the user.
- Rejected: a blue frame around the focused pane (the lit title row says it without a second edge), a colour on every
  row (colour is kept for status, type and the lamp), and a quit cross beside the pane actions.
