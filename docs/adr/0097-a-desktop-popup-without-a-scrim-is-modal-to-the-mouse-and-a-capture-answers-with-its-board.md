# 97. A desktop popup without a scrim is modal to the mouse, and a capture answers with its board

Date: 2026-11-07

## Status

Accepted. Amended by ADR 0108: every popup, the capture one included, now has a scrim. Carries ADR 0051's modal popups into the desktop window, which that ADR left to the browser.

## Context

The desktop window's capture popup is drawn without a scrim, so the Board and the lamps stay readable behind it. The other
dialogs block clicks with their scrim. With no scrim, the window's keys respected the popup, but a click on a ticket, a window
tab or a dock tab still reached what was behind it. ADR 0051 asks for popups that are modal to keys and mouse alike, reading
one answer to "is a popup open".

A captured ticket was selected on the first `board/changed` after the capture's answer. A watch read that began before
`gh issue create` returned could be sent after that answer, use up the selection on a board without the ticket, and leave the
new ticket unselected. To make the push follow the answer, every notification had to wait one microtask in the protocol
client, which touched every context's pushes.

## Decision

- **Clicks are stopped at the window, not by a scrim.** While a popup is modal, a capture-phase listener on the window stops
  every pointer event (press, release, click, wheel) whose target is outside a `[data-modal]` surface, and prevents the press
  so it does not take focus from the popup's input. The `Popup` and the `Dialog` scrim both carry `data-modal`, so scrimmed
  dialogs work as before. Rejected: a transparent scrim, which would satisfy the click rule but sits in the layout as an element
  the Design direction says is not there; and scoping ADR 0051 out of scrim-less popups, which lets a click change the screen
  under an open popup.
- **The keys and the clicks read one answer.** `KeyRouter.isModal()` says whether a popup holds the keys. The window's key
  routing and the click listener (through `AppModel.isModal()`) both read it, so the two cannot drift apart.
- **`board/capture` answers a captured ticket with the board read once GitHub took it.** `CapturedOnBoardView` carries the
  ticket's id and a `BoardAfterCaptureView`: that board, or `NoBoardAfterCaptureView` when the read failed, in which case the
  frontend keeps the board it shows. The sidecar reads it under the lock the watch's pushes take, so a read that began before the
  create is sent before the answer. The frontend shows that board and selects the ticket on it; no `board/changed` is the
  capture's. Rejected: marking the post-capture push as the capture's, a field on a general notification that only one caller
  reads; and leaving the race.
- **The protocol client dispatches a notification as soon as its line is read**, as it did before the capture: nothing waits
  on the order of an answer and a later push any more.

## Consequences

- A new popup drawn without a scrim is modal to the mouse only once `KeyRouter.isModal()` covers it; one it leaves out takes
  keys from behind it too, so the gap shows on both.
- The terminal's capture is unchanged: `TicketCaptureCommand` still answers a `TicketCaptureView`, and only the sidecar
  composes the board read into the desktop's answer.
- A watch read that began before the create is sent before the answer, so it cannot replace the capture's board. If GitHub
  has not listed the issue yet, a read made after the create can miss it, and the board lacks it until the next change.
