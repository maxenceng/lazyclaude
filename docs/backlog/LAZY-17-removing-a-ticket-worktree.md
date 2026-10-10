---
id: LAZY-17
status: done
context: cross-context
type: decision
created: 2026-10-10
blocked_by: [LAZY-1, LAZY-7, LAZY-9]
---

# When is a ticket worktree removed, by whom, and what if it holds uncommitted work?

## Question

When is a ticket worktree removed, by whom, and what if it holds uncommitted work?

LAZY-7 creates one ticket worktree `../<repo>.worktrees/<id>-<slug>` before a ticket's first session and recreates it if it's gone. Nothing yet removes it. The candidates:
- lazyclaude offers removal once the ticket's PR is `MERGED` (LAZY-9 already asks `gh`), or once the ticket is `done`;
- `/ticket done` removes it, so lazyclaude never does (check what the claude-tools `/ticket` skill already does);
- nobody does: the user runs `git worktree remove`, consistent with "git operations: lazygit exists".

And when it holds uncommitted or unpushed work, or a live hosted session sits in it?

## Kind

grilling

## Resolution

Grilled on 2026-10-10, against ddd-workflow 2.2.2's `/ticket`.

**lazyclaude removes it, only on the user's yes, once the ticket is `done` and the worktree holds nothing to lose.**

- **Not on `MERGED`.** `/ticket done` runs after the merge, in the ticket worktree (LAZY-7), and writes its close-out (`status: done`, notes, traces) on a branch and PR. Removing on `MERGED` would pull the directory out from under it.
- **lazyclaude, not `/ticket`, not nobody.** lazyclaude created the worktree, so it owns its end. `/ticket done` touches no worktree today and stays that way: it runs inside one, and `../<repo>.worktrees/` is lazyclaude's layout. "Git operations: lazygit exists" covers operations the user starts, not cleanup of what lazyclaude made.
- **When it's offered:**
  - when a hosted `/ticket done` session exits and the worktree's file reads `done`, lazyclaude asks "remove worktree `<path>`? y/n";
  - otherwise `d` on a Board ticket that reads `done` and still has a worktree (lazygit's key for removing a worktree). `d` isn't shown in the key bar for any other ticket.
  - No startup sweep.
  - An open close-out PR doesn't block: its commits are pushed.
- **Refused, with the reason in the key bar, when any of these holds:**
  - `git status --porcelain` in the worktree isn't empty, untracked files included (the paths are listed);
  - commits on no remote ref that the merged PR doesn't cover, meaning beyond its `headRefOid`. Plain `--not --remotes` is wrong after a squash merge whose head branch GitHub deleted;
  - any live session, hosted or outside, has its `cwd` in the worktree (the sessions are named).

  There's no force, no stash, and lazyclaude never ends a session to make room.
- **Removal is `git worktree remove` only.** The local ticket branch stays: `git branch -D` is lazygit's job.
- **LAZY-9 amended:** a ticket branch whose PR is `MERGED` stops being a status source. The ticket worktree's file still counts while the worktree exists, whatever branch it's on (the close-out branch reads `done`). Otherwise status comes from the root copy. Without this, a kept branch would show a finished ticket as `in-review` forever. Glossary *Branch copy*, *Status* and *Ticket worktree* updated.
- **Shown:** the ticket detail gives the worktree's path and state (clean / N dirty paths / N unpushed commits). Nothing appears on the board.
- **Resume after removal** still recreates the worktree at its path (LAZY-7), because Claude Code finds a transcript by its `cwd`. The user can remove it again.
- No ADR: every part of this is cheap to reverse.
