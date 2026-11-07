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
  section shows only while no ticket is selected, unchanged: the newest five past sessions, the rest behind the
  older-sessions row.
- **The age leaves the session views.** `LiveSessionView` and `PastSessionView` no longer carry it, so neither the protocol
  nor the generated types do. The terminal asks `SessionAgeQuery` for a row's age when it draws it, measured from the
  `startedAt` or `lastActivity` the view already carries; the protocol keeps writing every component of a view, with no
  exception.
- **An age is measured when it is drawn**, not frozen at the read: it moves on between reads, and no longer makes every row
  of one read agree on the same now.

## Consequences

- An unlinked session can be resumed only with no ticket selected; in the desktop window that is an empty board.
- `SessionAgeQuery`'s operations are listed as asked only by the terminal; giving the desktop an age again means a
  protocol method or a view component, and a formatted one.
