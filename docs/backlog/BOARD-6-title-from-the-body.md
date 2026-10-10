---
id: BOARD-6
status: draft
context: board
type: chore
created: 2026-10-10
---

# The ticket derives its title from its body, rather than the repository deriving both from the same lines

## Acceptance criteria

_Not analysed yet — `/ticket refine BOARD-6`._

## Model decision

_Not analysed yet — `/ticket refine BOARD-6`._

## Design direction

_Not analysed yet — `/ticket refine BOARD-6`._

## Glossary impact

_Not analysed yet._

## Notes

Raised in review of [BOARD-2](BOARD-2-ticket-detail.md) and deferred there: `BacklogRepository` reads the title and the body from the same lines, so they cannot disagree today, but the rule (first `#` heading outside a code fence) is a domain rule living in a secondary adapter.
