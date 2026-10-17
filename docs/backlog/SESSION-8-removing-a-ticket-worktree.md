---
id: SESSION-8
status: done
context: session
type: feature
tracker_id: 85
created: 2026-10-10
---

# Removing a ticket worktree on a y/n, with its refusals

## Acceptance criteria

- [x] `d remove worktree` shows in the key bar only on a Board ticket that reads `done` and still has its ticket worktree; on any other ticket `d` is neither shown nor bound.
- [x] `d` runs the checks off the render thread; when all pass, the status line asks `remove worktree <path>? y/n`. `y` runs `git worktree remove` and keeps the local ticket branch; `n` or Esc leaves everything as it was.
- [x] When a hosted session running `/ticket done` exits and the ticket worktree's file reads `done`, the same checks run and the same question is asked. A hosted session running any other verb, or one whose ticket does not read `done`, offers nothing on exit.
- [x] Removal is refused, with the reason in the key bar and no question asked, when `git status --porcelain` in the worktree is not empty, untracked files included — the paths are listed.
- [x] Removal is refused when the worktree holds commits beyond the merged pull request's head that no remote ref has.
- [x] A worktree whose ticket was squash-merged and whose head branch GitHub deleted, with nothing committed since, is not refused — the case plain `--not --remotes` gets wrong.
- [x] Removal is refused when no merged pull request is found for the ticket branch (none, closed unmerged, or beyond the merged read's reach), with "cannot tell what the merge covered".
- [x] Removal is refused when any live session, hosted or outside, has its `cwd` in the worktree — the sessions are named. lazyclaude never ends a session to make room.
- [x] When `git worktree remove` itself fails after a yes, git's message shows in the key bar and the worktree, branch and screen are left as they were.
- [x] The ticket detail shows the ticket worktree's path and its worktree state: clean, N dirty paths, N commits beyond the merge, or no merged pull request. The Board shows nothing of it.

## Model decision

New behaviour on the existing ticket worktree, in `session`, which creates it (ADR 0030). No new aggregate, no new context.

- `session`: `WorktreePort` gains the dirty paths (`git status --porcelain`), the commits beyond a given head on no remote ref, and `git worktree remove`. A session domain manager composes those with the live sessions' `cwd` into a *worktree state*, and removes only when that state is clear. The state that refuses and the state the detail shows are the same value — one source.
- `review`: publishes the merged pull request's head commit per branch. `GhMergedPullRequestsRepository` already reads `headRefOid` into `HeadCommit` and drops it; `session` reads it through `review.published`, not a second `gh` call.
- `board` stays the owner of `done`. On a hosted session's exit, `session` tells the shell which ticket's `/ticket done` ended (the verb is in `HostedSession`'s `TicketClaude`, today lost at `exited`); the shell checks board's status before offering. `session` never reads `board`.
- `shell`: the `d` key, its key-bar entry, and the y/n on the status line, beside the quit and kitty.conf prompts.

## Design direction

No new screen. The key bar gains `d remove worktree` on a qualifying Board ticket; the question and the refusals use the status line and key bar the way the quit and kitty.conf prompts do.

```
 remove worktree ../lazyclaude.worktrees/PROJ-1-slug? y/n
 cannot remove: 2 dirty paths — src/Foo.java, notes.txt
 cannot remove: live session "PROJ-1 start" is in the worktree
```

The ticket detail adds one row under the branch: `worktree  ../lazyclaude.worktrees/PROJ-1-slug  clean`.

## Glossary impact

- *Worktree state* (`session`): a ticket worktree's dirty paths, its commits beyond the merged pull request, and the live sessions in it; clear when all three are empty, or *no merged pull request* when the merge cannot be read.
- *Merged head* (`review`): the head commit (`headRefOid`) of a branch's merged pull request.

*Ticket worktree* already states the removal rule; it gains a link to *Worktree state*.

## Notes

Charted on the [LAZY-1](LAZY-1-v1-terminal-ide.md) map. Decided in [LAZY-17](LAZY-17-removing-a-ticket-worktree.md). The commits-beyond-the-merged-head refusal needs the merged PR's `headRefOid` from REVIEW-1.

Closed after [#35](https://github.com/maxenceng/lazyclaude/pull/35). ADR 0038 records the merged head read once in `review`, the `rev-list` that survives a squash-merge, and the exit callback. What the code will not show:

- *Model decision* names two refusing states. Review added a third, *no sessions read*: when neither the registry nor `claude agents --json` answers, removal is refused, since the hosted sessions alone would miss a `claude` started outside lazyclaude. ADR 0038 and the glossary carry it. *Model decision* does not.
- The architecture reviewer worried that the `/ticket done` session that just ended would block removal of its own worktree. It doesn't, because `exited()` refreshes the sessions before the done listener fires. That order is the whole guarantee, and one test pins it.
- Kept under review on purpose: `CommitId` (the state holds the commits and the detail counts them), the `WorktreeContents`/`HeldWorktree` names beside the glossary's *Worktree state*, and `done` checked only in the shell.
