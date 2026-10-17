# 38. A ticket worktree is removed on a yes, judged by its worktree state

Date: 2026-10-17

## Status

Accepted. Carries out LAZY-17's decision; builds on ADR 0030 (the ticket worktree belongs to `session`).

## Context

LAZY-17 decided that lazyclaude removes a ticket worktree only on the user's yes, once the ticket is `done` and nothing in the worktree
would be lost. Carrying it out needed three choices the decision left open.

## Decision

- **The merge is read once, in `review`, and handed over.** `review` publishes `MergedHead` (`HeadCommit` | `NoMergedHead`) and its
  `MergedPullRequestsView` answers it per branch. The shell hands it to `session` in the payload, as it hands the merged flag for the
  verb picker. `session` makes no second `gh` call and never reads `board`. With no merged head the state is *no merged pull request*:
  nothing is clear to remove.
- **Commits beyond the merge are `git rev-list HEAD ^<merged head> --not --remotes`.** The head is negated with `^` because it comes from
  outside and no option begins with it. Plain `--not --remotes` would count every commit of a squash-merged ticket whose head branch GitHub
  deleted. A head git does not know is a failed read, not a clear worktree.
- **The ticket worktree is the directory at the project's path that git lists**, whatever branch it is on, since the close-out branch is
  checked out there too. `WorktreeState` is one value: the detail row, the refusal and the removal all read it, and the removal judges it
  again at the yes. Live sessions are counted from the watcher's last push with the hosted ones as they are now (a session started in the
  worktree within one tick is not seen: a `deferred:` on `worktreeOf`); the registry being unreadable leaves the hosted ones.
- **A hosted `/ticket done` session's end reaches the shell as a callback** (`SessionQuery.onTicketDoneEnded`), raised by
  `HostedSessionManager` only for a session started with that verb, however it exited. The shell reads the board again, and only when
  the ticket now reads `done` runs the same check as `d`. `session` never reads `board`; `board` stays the owner of `done`.
- **`d` is bound on the Board only for a `done` ticket whose worktree was last read as present.** The shell reads the selected ticket's
  worktree off the render thread whenever the selection, the sessions, the merged heads or the board change, one read at a time; a read
  is kept with its ticket, so one that lands after another ticket was selected shows nowhere. `d`, the removal and the exit-triggered
  check run off the render thread, chained after one another.

## Consequences

- A merge older than the 100 newest merged pull requests reads as *no merged pull request*, so its worktree is never offered for removal.
- Each selection of a ticket runs `git worktree list`, `git status` and `git rev-list` once, off the render thread.
- Untracked files make a worktree dirty, so a scratch file blocks removal until the user deals with it; nothing is forced or stashed.
