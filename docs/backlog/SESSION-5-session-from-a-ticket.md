---
id: SESSION-5
status: draft
context: session
type: feature
tracker_id: 82
created: 2026-10-10
---

# Starting or resuming a session from a ticket: the verb picker with a preselected verb, run in the ticket worktree, which is created if missing

## Acceptance criteria

_Not analysed yet — `/ticket refine SESSION-5`._

## Model decision

_Not analysed yet — `/ticket refine SESSION-5`._

## Design direction

_Not analysed yet — `/ticket refine SESSION-5`._

## Glossary impact

_Not analysed yet._

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-9](LAZY-9-likeliest-next-verb.md), [LAZY-7](LAZY-7-handing-over-to-a-session.md), [LAZY-15](LAZY-15-trusting-ticket-worktrees.md). The worktree is `../<repo>.worktrees/<id>-<slug>`. The PR-state refinement of the preselected verb needs REVIEW-1's merged state. The worktree's path rule already lives in `board` (`BoardManager.ticketWorktree`, from BOARD-3): share it — likely beside `Project` in `shared.domain` — rather than writing a second copy.
