# 28. A root copy reading `done` wins over any branch copy

Date: 2026-10-15

## Status

Accepted. Amends the status source decided in LAZY-9 and amended in LAZY-17.

## Context

A ticket's status is read from its branch copy whenever there is one, because `/ticket` commits each status change on
the ticket branch and the root copy reads `todo` until the merge. LAZY-17 stopped a ticket branch whose pull request is
merged from being a source, so a kept local branch would not show a finished ticket as `in-review` forever.

That rule rests on knowing which pull requests are merged, and `board` learns it from `review`'s `gh` fetch: one call at
startup, then every 60 s, over the 100 most recent merged pull requests. Until the first answer, whenever GitHub cannot
be read, and for any branch merged before those 100, a kept branch is not known to be merged and counts as a source.
This repo keeps its merged ticket branches locally, so every finished ticket would read `in-review` at every startup and
whenever lazyclaude runs offline.

## Decision

- **A root copy reading `done` wins over any branch copy.** It is checked before any worktree or `git show` is read. A
  ticket only reaches `done` in the root checkout through the close-out's merge, after which no branch copy can be more
  current, so nothing is lost.
- **Otherwise the source order stands:** a worktree's file — the worktree checked out on the ticket branch, the root
  checkout included, or the ticket worktree whatever branch it is on — then `git show <ticket branch>:<file>` while the
  branch is not in the merged set, then the root copy.
- **An unknown merged set is an empty one.** Until the fetch answers, or while GitHub cannot be read, every ticket branch
  counts as a source; the board never waits on `gh`, and no warning is raised, since the root `done` rule already
  covers the case that would read wrong.

## Consequences

- Removing this rule brings back an `in-review` for every finished ticket with a kept branch, on each start and offline.
  It is the reason the merged set may be stale without being wrong.
- A ticket reopened on its branch after reaching `done` on `main` still reads `done` until the reopening merges. Nothing
  in `/ticket` reopens a ticket, so this is accepted rather than handled.
- A branch merged before the 100 most recent pull requests, whose ticket was never closed, still counts as a source and
  shows its last branch status. Its root copy is older still, so the branch copy is the better of the two.
