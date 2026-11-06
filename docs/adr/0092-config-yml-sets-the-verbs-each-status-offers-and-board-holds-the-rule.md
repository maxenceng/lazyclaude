# 92. `config.yml` sets the verbs each status offers, and `board` holds the rule

Date: 2026-11-06

## Status

Accepted. Amends ADR 0030: `session` reads `Status` and `StatusVerbs` from `board.published` beside `Verb`. Builds on ADR
0078: the section is read once at start-up.

## Context

The verbs a ticket offers were fixed in `Status`. A project that wants `done` ahead of `respond`, or no verb on `todo`, had no
way to say so. `config.yml` has one reader, `session`'s `ConfigurationNodes`, which is the only place a refusal can name its
`file:line`; `board` owns what a ticket offers.

## Decision

- **`session` parses.** `ConfigurationNodes` reads `verbs:` into `StatusVerbs` (status, verbs in order), a published type of
  `board`. It refuses at start-up, with its line: an unknown status label or verb, a verb twice under one status, `new`,
  `refine` outside `draft`, `start` outside `todo`, `design` outside `in-progress`, any verb under `done`. The gates are
  `Verb.mayBeOfferedIn`, mirroring only what `ticket-tracker ready` enforces. `session` reads `Status` and `StatusVerbs`
  from `board.published`, an edge added to the context map.
- **`board` holds the rule.** `OfferedVerbs` in its domain answers a status's verbs: the configured list, or `Status.verbs()`
  for a status the section leaves out; an empty list leaves a status no verb. `BoardManager` reads the configured lists
  through `ConfiguredVerbsPort`, implemented by `ConfiguredVerbsRepository`, and gives each `Ticket` its status's verbs.
  `Ticket.verbs()` keeps the `frontend/**` gate on `design`, wherever it is listed. `Status.IN_PROGRESS` now lists `design`
  after `review`, so the gate is the only thing that holds it back.
- **`Contexts.of` hands the mapping over.** The mapping is read with the rest of `config.yml` before any context is built, and rides
  `ConfigurationView.statusVerbs` to `Contexts.of`, once for the shell and the sidecar. It is on the wire, so the protocol
  schema and the desktop's generated types carry it; the window reads verbs from each ticket and ignores it.
- **`session` keeps preselection.** `VerbChoice` still prefers `design` not yet run, then `done` on a merged branch, else the
  first verb listed, which is the list's order.

## Consequences

- No watcher: editing `verbs:` takes effect on the next start, like every key.
- A verb outside the closed set `/ticket` names cannot be configured in.
- A status's verbs belong to no per-project file; `config.yml` is per user.
