# 100. A done ticket offers `done`, and the desktop's Base lists the ten latest closed

Date: 2026-11-07

## Status

Accepted. Amends ADR 0092 (what `done:` in `config.yml` may hold) and ADR 0093 (Base no longer folds done, and a done card
goes to Base). The TUI keeps its folded done column.

## Context

A merge closes a ticket's issue, and the board then reads it done. `/ticket done` is the step run after that merge, yet a done
ticket offered no verb, so `n` on it started an unlinked session, and the desktop's Base folded done to its count, so the
ticket could not even be selected there. The Board tab listed the ten latest closed but could only preview them.

## Decision

- **Only `done` once finished.** `Verb.mayBeOfferedIn` lets `done` be offered under every status and every other verb only
  under an open one; `refine`, `start` and `design` keep `ticket-tracker ready`'s gates. `Status.DONE` offers `done` by default.
- **`done:` in `config.yml` holds `done` or nothing.** `done: [done]` and `done: []` are read; any other verb under `done` is
  refused with its line, by the same refusal as a verb out of its status ("start is only offered under todo, not done").
- **Base lists done as the Board tab does**: its ten latest closed, in the board's order, under the heading that still counts
  them all, last after the live groups, its rows reached by the cursor like any other. A done card on the Board tab goes
  to Base on `enter` or a second click, as every other card does.
- Rejected: a `done` offered only on a done ticket whose worktree is still held. The verb is what `/ticket` accepts; whether
  there is anything left to clean up is the `done` step's own question.

## Consequences

- `n` on a done ticket, in either frontend, opens the verb picker with `done`; `done: []` brings back the unlinked session.
- Base's Board panel grows by up to ten rows under done; the live groups still head it, so nothing under way is pushed down.
- The TUI's done column stays folded: a done ticket is reached there only by unfolding it, as before.
- A done ticket older than the ten is in neither tab; its `done` step is run from a session started some other way.
