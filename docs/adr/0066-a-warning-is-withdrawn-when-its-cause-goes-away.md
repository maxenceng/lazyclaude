# 66. A warning is withdrawn when its cause goes away

Date: 2026-10-30

## Status

Accepted. Amends ADR 0022, which keeps every warning until quit.

## Context

ADR 0022 holds a warning "until lazyclaude quits", and a manager names each degraded mode once. That suits a mode that does
not end: a watch that fell back to polling stays polling. A failed board read ends: the next good read puts the board
back, and a warning still on the status line then says something untrue. BOARD-8 asked for it to clear.

## Decision

- **`WarningPort` gains `withdraw(Warning)`.** It takes back one raised warning equal to the one given. A warning that is
  not held is ignored, and two equal warnings are two, so withdrawing one leaves the other.
- **The watch side reports a withdrawal.** `WarningWatchPort.subscribe` still hands the listener the warnings that remain,
  non-empty as `Warnings` always is. A second hook, `onNoneHeld`, runs when the last one goes, and the status line empties.
  Rejected: letting `Warnings` be empty, which gives every reader of `newest()` a case it never had.
- **`WarnOnce` remembers its warning and can withdraw it.** `raise` still says a mode once; `withdraw` takes back what it
  raised and lets the mode be named again, so a failure after a recovery is a new warning rather than silence.
- **Only a mode whose cause can end withdraws.** The board manager withdraws its read warning on the next good read; with
  no earlier failure that read touches nothing. Every other warning is raised as before and stays until quit.

## Consequences

- A manager that wants to withdraw must hold the `WarnOnce` it raised through and call `withdraw`; nothing withdraws on a
  timer or by text.
- The count on the status line goes down when one of several is withdrawn.
