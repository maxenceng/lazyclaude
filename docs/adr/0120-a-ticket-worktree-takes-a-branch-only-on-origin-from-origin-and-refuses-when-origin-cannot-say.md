# 120. A ticket worktree takes a branch only on origin from origin, and refuses when origin cannot say

Date: 2026-11-20

## Status

Accepted. Amends ADR 0030's "on the branch, or on a new branch from `main`" bullet; the rest of ADR 0030 stands. Builds on ADR 0118.

## Context

ADR 0118 made a ticket's branch the one GitHub links to its issue, and GitHub can link a branch that exists only on the remote:
one pushed from another machine, or made by `gh issue develop` without a checkout. `WorktreeRepository.branchExists` reads
`refs/heads` only, so for such a branch the ticket worktree was added on a new branch from local `main`. The session then ran on
none of the branch's commits, and its first push would have diverged from the work it was meant to continue (issue #249).

## Decision

- **Local first, then origin, then `main`.** Adding a ticket worktree, whether starting a ticket session or resuming into a removed
  ticket worktree, uses the local branch when there is one, and asks nothing of the remote. With none, it asks origin with
  `git ls-remote --exit-code --heads origin refs/heads/<branch>`: exit 2 means origin lacks it, and the worktree goes on a new
  branch from `main` as before. With origin holding it, `git fetch origin refs/heads/<branch>:refs/remotes/origin/<branch>` and
  `git worktree add --track -b <branch> <path> refs/remotes/origin/<branch>` put the worktree at origin's tip, on a local branch
  that tracks it.
- **The full ref is asked for.** `ls-remote` matches a pattern against the end of a ref, so the bare branch name would also find
  `x/<branch>`.
- **The remote is fetched at launch.** Launching a session is the only moment the branch is needed, and the start already runs off
  the input thread in both frontends, so the fetch delays that start and nothing else. No background fetch is kept.
- **It fails closed.** When origin cannot be asked (offline, refused, no remote named `origin`) or the fetch fails, nothing
  starts and the warning is `ticket worktree: <git's first line>; nothing started.`, like any other refused worktree. A branch
  origin may hold is never replaced by a fresh one from `main` on a guess. For a remote's failure, git's first line is the reason
  (`fatal: 'origin' does not appear to be a git repository`) and the lines after it are advice, so that line is the one shown, where
  the local commands show their last.
- **Git never prompts.** Every `git` run `WorktreeRepository` makes carries `GIT_TERMINAL_PROMPT=0`, so an HTTPS remote that wants
  credentials fails with a reason instead of writing a prompt over the terminal and waiting for an answer nobody is asked for.
  The local commands never prompt, so one setting covers them all.
- **The remote is `origin`, hardcoded**, as `main` already is.
- Rejected: falling back to a branch from `main` when origin cannot be asked, which is the very divergence this fixes, now
  only offline; fetching on every board read, which pays a network call for a branch no one is starting.

## Consequences

- deferred: a project whose GitHub remote is not named `origin` cannot start a session on a ticket whose branch is not local —
  close it when a project names its remote otherwise, by reading the remote `gh` resolves.
- deferred: an SSH remote whose key needs a passphrase and has no agent can still ask for it on the terminal, since
  `GIT_TERMINAL_PROMPT` covers only git's own prompt — close it when that is seen, by running the remote commands with
  `BatchMode=yes`.
- deferred: a remote that never answers holds the start until git gives up, since `GitCommand` has no timeout — close it when a
  start is seen hanging.
- Starting a session on a ticket with no local branch now needs the network, even when origin turns out not to have the branch.
