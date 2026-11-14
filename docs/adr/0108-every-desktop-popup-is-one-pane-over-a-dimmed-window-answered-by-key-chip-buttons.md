# 108. Every desktop popup is one pane over a dimmed window, answered by key-chip buttons

Date: 2026-11-14

## Status

Accepted. Builds on ADR 0103's visual system. Amends ADR 0097: the capture popup now has a scrim, so the reason that ADR's
Context gives for having none no longer holds. ADR 0097's decision stands: clicks are stopped at the window through
`[data-modal]` and `KeyRouter.isModal()`, not by the scrim.

## Context

The desktop window drew its popups two ways. The capture popup was a focused `Pane` floated with no scrim, so the board stayed
readable behind it. The quit confirmation, the review's comment box and its submit confirmation were a `Dialog`: a plain box on
a scrim, with a dim hint line saying which keys answer. The two shapes looked unrelated to each other and to the panes of
ADR 0103, and the only way to answer either was a key, except for the capture's text field. A throwaway prototype drew one
frame three ways (issue #221, variants P1 to P3), and P2 was chosen.

## Decision

- **One design-system popup frame replaces `Popup` and `Dialog`.** It is a pane like the window's others: a lit title row
  that asks the question, with a close action (`esc`) in the pane-action slot; a body for what the question is about, left
  out when there is nothing to show; and a sunk foot holding the answers. It is centred over the window, which a scrim of
  `--ground` at 55% dims.
- **The capture popup has a scrim too.** Rejected: keeping it scrim-less and high under the tabs (P1). With the window
  behind it at full strength, two lit title rows showed at once, the popup's and the focused pane's. The scrim leaves only
  one. The board is still visible through it, only dimmer.
- **Each answer is a button carrying its bound key as a chip**, the main answer in the affirm tone at the foot's right end,
  the way out to its left. A button calls the operation its key already calls. The hint line is gone, because the chips
  replace it. Rejected: a lifted card with a coloured top edge and no lit row (P3), which looked less like the window's
  panes.
- **A press outside a popup behaves as before.** It cancels the quit confirmation and the comment box, and does nothing on
  the submit confirmation or the capture popup. The scrim carries `data-modal`, so ADR 0097's window listener lets a press
  on it through to the frame and stops one that would reach anything behind.

## Consequences

- A new popup gets the frame and its buttons for free. If it answers to a key, it needs a button with that key's chip
  (the rule that every desktop action has a button and a key).
- Because the capture popup now has a scrim, the scrim is a single place where a press outside could cancel it. It does
  not, because a press there would throw away a title half typed.
- The session picker and the window menu are dropdowns anchored to what opened them, not popups, and keep their own shape.
