---
id: BOARD-3
status: in-review
context: board
type: feature
tracker_id: 74
created: 2026-10-10
---

# Status comes from the branch copy: the worktree file, else `git show`, ignoring branches whose PR is merged

## Acceptance criteria

- [ ] A ticket with a ticket branch shows its branch copy — status, title and body — on the
      board and in the ticket detail.
- [ ] The branch copy is the worktree's file when a worktree has the ticket branch checked out,
      the root checkout included, or sits at `../<repo>.worktrees/<ticket branch>` whatever
      branch it is on; an uncommitted edit there shows. With no such worktree it is
      `git show <ticket branch>:docs/backlog/<file>`.
- [ ] A ticket branch in the merged set is not a source: the root copy is shown.
- [ ] A root copy reading `done` wins over any branch copy, so a kept local branch never brings
      back an old status — not at startup, not offline, not for a PR beyond the 100 most recent
      merged ones.
- [ ] Until the merged fetch answers, or while GitHub cannot be read, a ticket branch still
      counts as a source. Nothing blocks and no warning reaches the status line.
- [ ] A ticket file missing from the branch or worktree falls back to the root copy, silently.
- [ ] A branch copy whose frontmatter or status is refused, or a `git show` that fails, lists
      the ticket as an unreadable ticket with the reason; it never falls back to the root copy.
- [ ] A branch that only shares the id prefix (`PROJ-10-…` for `PROJ-1`), or is not a ticket
      branch (`close-PROJ-1`), is never a source.
- [ ] The merged fetch is one `gh pr list --state merged --limit 100 --json
      headRefName,headRefOid,number` call at startup, then every 60 s; two runs never overlap.
      Fork PRs are left out, as the open-PR fetch does.
- [ ] When the set of merged ticket branches changes, the board is read again and the selection
      stays on the same ticket id.

## Model decision

New behaviour on `board`'s existing read — no new aggregate, no new context. `context: board`
stays: the behaviour is a ticket's status; `review` and `shell` only supply it.

- **Board.** A branch-copy port, implemented in `board.infrastructure.secondary` by a git
  repository running `git worktree list --porcelain` and `git show` (the first git reads
  outside `ProjectLocatorRepository`; `GitFixture` builds the repos for its tests).
  `BoardManager` picks each ticket's source: the root copy if it reads `done`; else a
  worktree's file (checked out on the ticket branch, root checkout included, or at the ticket
  worktree's path); else `git show` if the branch exists and is not merged; else the root
  copy. The whole ticket comes from that source (ADR 0014: the detail rides the board read).
  `BoardPayload` carries the merged ticket branches as plain names — `board` still knows
  nothing of pull requests (LAZY-21).
- **Review.** A merged-PR port and `gh` repository beside the open-PR ones, on the same 60 s
  daemon timer, with the same checking / unavailable states. It keeps `headRefOid` for
  SESSION-8's refusal and the PR number for SESSION-5.
- **Shell.** Subscribes to the merged state; when the set of merged branches changes it reads
  the board again through `BoardQuery`, selection following the ticket id. Today the board is
  read once at startup, so without this re-read the merged set would change nothing until a
  restart. File and ref watchers and `R` stay with BOARD-4.
- **ADR.** "A root copy reading `done` wins over any branch copy" amends LAZY-17's rule, and is
  the kind of rule someone would remove without knowing why — it gets an ADR in this change.

## Design direction

n/a — nothing new on screen; statuses become correct.

## Glossary impact

- *Branch copy* drops its *(expected)* marker.
- *Status* gains: a root copy reading `done` wins over any branch copy.
- *Merged pull request*, new in `review`: a pull request on this repository, not from a fork,
  GitHub reports merged. *Pull request* stays the open one.
- Nothing displaced.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-9](LAZY-9-likeliest-next-verb.md), amended by [LAZY-17](LAZY-17-removing-a-ticket-worktree.md). The merged branches are handed in by `shell` as branch names ([LAZY-21](LAZY-21-keeping-the-screen-current.md)). REVIEW-1's refinement moved the merged-PR fetch here (`gh pr list --state merged --limit 100 --json headRefName,headRefOid,number`, in `review`, on REVIEW-1's schedule), since this is its first reader; SESSION-5 and SESSION-8 take it from here.

Refined 2026-10-15. Root `done` winning was chosen over a stale-status `// deferred:` because this repo keeps merged local ticket branches (`BOARD-1-board-from-the-backlog`, …), which would otherwise read `in-review` at every startup and whenever GitHub is unreadable. Ticket branches are checked out in the root checkout here, not in `../<repo>.worktrees/`, which is why a worktree on the branch counts wherever it is.
