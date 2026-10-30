# 68. A roadmap is a dated document in docs/roadmap/, part of the record

Date: 2026-10-30

## Status

Accepted. Amends ADR 0055, under which `ROADMAP-v1.1.md` left with `docs/backlog/` and became a pinned issue. Widens
ADR 0053's record.

## Context

`docs/backlog/ROADMAP-v1.1.md` orders the desktop evolution's tickets. Deleting `docs/backlog/` (ADR 0055) left the plan
for its unbuilt steps without a home, and the plan was a pinned *Roadmap v1.1* issue, a checklist of issue numbers. But
every issue is read as a ticket: the board shows an open issue with no `status:` label as a draft, and `/ticket` lists
it among the drafts. A roadmap is not a ticket. It is a plan written on a date, read next to the ADRs, and the docs site
is to publish it with them.

## Decision

- **A roadmap is a markdown document under `docs/roadmap/`, one per version, `v1.1.md` the first.** It lists the
  version's unbuilt steps in build order, each linking its issue. The Docs panel lists it, and the site publishes it with
  the ADRs. Rejected: a pinned issue, which the board and `/ticket` read as a draft ticket; a roadmap label the board
  skips, which is a rule in `board` for one issue.
- **`docs/roadmap/` is part of the record**, with `docs/adr/`, `docs/research/`, `docs/endpoints/` and `scripts/`.
  `make ticket-check` lets it name an issue by `#n` or URL, as an ADR may. Rejected: naming each step by its issue's
  title only, which keeps the rule whole but makes every step a search.

## Consequences

- A step that lands is not ticked here. The roadmap is a dated plan, and an issue's state says what is built.
- A new version's roadmap is a new file, and the old one stays as written.
