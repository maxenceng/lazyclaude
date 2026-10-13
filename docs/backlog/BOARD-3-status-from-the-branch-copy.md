---
id: BOARD-3
status: draft
context: board
type: feature
tracker_id: 74
created: 2026-10-10
---

# Status comes from the branch copy: the worktree file, else `git show`, ignoring branches whose PR is merged

## Acceptance criteria

_Not analysed yet — `/ticket refine BOARD-3`._

## Model decision

_Not analysed yet — `/ticket refine BOARD-3`._

## Design direction

_Not analysed yet — `/ticket refine BOARD-3`._

## Glossary impact

_Not analysed yet._

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-9](LAZY-9-likeliest-next-verb.md), amended by [LAZY-17](LAZY-17-removing-a-ticket-worktree.md). The merged branches are handed in by `shell` as branch names ([LAZY-21](LAZY-21-keeping-the-screen-current.md)). REVIEW-1's refinement moved the merged-PR fetch here (`gh pr list --state merged --limit 100 --json headRefName,headRefOid,number`, in `review`, on REVIEW-1's schedule), since this is its first reader; SESSION-5 and SESSION-8 take it from here.
