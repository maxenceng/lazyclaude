# 96. A selected ticket's sessions are only its own, and the desktop shows no session age

Date: 2026-11-07

## Status

Accepted.

## Context

Under a selected ticket, the sessions panel listed the ticket's sessions and then every unlinked one: probe sessions,
interrupted ones, anything ever run in the project. Choosing what to resume for a ticket meant reading past sessions that had
nothing to do with it. Separately, the desktop window showed each session's age as the raw ISO-8601 duration the protocol
carried (`PT3H12M5S`), because the sidecar writes the same views the terminal reads, every component as it is (ADR 0050).

## Decision

- **A selected ticket lists only the sessions linked to it**, in the terminal and the desktop window alike. The Unlinked
  section shows only while no ticket is selected, and each frontend's keeps what it held: the terminal's lists the unlinked
  live sessions and the newest five past ones, the rest behind the older-sessions row; the desktop's lists the newest five
  past ones and no more.
- **Which sessions a ticket lists is the `session` context's.** `SessionsView` carries, beside its live, past and hosted
  sessions, a `TicketSessionsView` for every ticket a session is linked to or a tab was started for, and an
  `UnlinkedSessionsView`. Each frontend picks the selected ticket's group, or the unlinked one with none selected, and only
  lays it out, so the two composers can no longer disagree on what belongs (ADR 0084's "a second composer moves the rule into
  the context"). The listing is carried on the view instead of being answered by a query per ticket, so the desktop picks its group
  on each cursor move without a round trip that could come back late.
- **The age leaves the session views.** `LiveSessionView` and `PastSessionView` no longer carry it, so neither the protocol
  nor the generated types do. The terminal asks `SessionAgeQuery` for a row's age when it draws it, with a
  `SessionAgePayload` holding the `startedAt` or `lastActivity` the view already carries, and draws the `SessionAgeView` it
  answers (ADR 0012's shape, with no exception); the protocol keeps writing every component of a view, with no
  exception.
- **An age is measured when it is drawn**, not frozen at the read: it moves on between reads, and no longer makes every row
  of one read agree on the same now.

## Consequences

- An unlinked session can be resumed only with no ticket selected: in the terminal, with the board's cursor on a row that
  is not a ticket; in the desktop window, which always selects the first ticket, only on an empty board.
- `SessionAgeQuery`'s operations are listed as asked only by the terminal; giving the desktop an age again means a
  protocol method or a view component, and a formatted one.
- A session started with `n` from a ticket with no verb left is unlinked (ADR 0084), so that ticket's panel does not list
  it. It is still reachable from its dock tab, and with no ticket selected.
- Every session is written to the protocol twice: once in its own list and once in its group. The views are small,
  and the desktop is spared re-deriving a rule it must not own.
- An age is elapsed time: the local time a view carries is read back in the clock's zone, so a clock change between it
  and now keeps its length; only a time inside the hour a change repeats is read at its earlier offset.
