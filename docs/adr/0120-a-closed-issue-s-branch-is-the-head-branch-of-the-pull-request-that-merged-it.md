# 120. A closed issue's branch is the head branch of the pull request that merged it, and a worktree at its path is its worktree

Date: 2026-11-20

## Status

Accepted. Closes ADR 0118's deferred consequence about a deleted branch, and amends its first bullet: with no branch linked, a
closed issue's branch is the head of the merged pull request that closed it, before the slug. Amends ADR 0030's ticket worktree
bullet: a worktree git lists at the planned path is the ticket's, whatever branch it has checked out. The rest of both stands.

## Context

This repository now deletes a branch when its pull request merges. GitHub then keeps no ref for it, so a done ticket's
`linkedBranches` is empty and ADR 0118 falls back to the title slug. Where the slug differs from the branch GitHub named,
`doesn-t` against `doesnt`, the ticket's branch is checked out nowhere. Its worktree is still at the slug's path, because
`<repo>.worktrees/<branch>` was planned from the slug when the worktree was added, but it has GitHub's branch checked out. ADR 0030
refused that path as "exists but is not a worktree of", so `/ticket done` could not be started on a merged ticket.

## Decision

- **A closed issue with nothing linked takes the head branch of the pull request that merged it.** The board's query adds
  `timelineItems(itemTypes: [CLOSED_EVENT], last: 1)` and, for a `ClosedEvent` whose `closer` is a pull request, its
  `headRefName` and `merged`. `IssueRecord` names the branch as the linked branch, else that head when the issue is `CLOSED` and
  the pull request merged, else the slug. It is still one paginated call. A closer that is a commit, a missing closer, or no
  close event at all reads as no merged pull request.
- **Only a closed issue.** A reopened issue keeps the slug, because its merged branch holds the earlier work and starting
  the ticket again would check that branch out.
- **The last close event, not `closedByPullRequestsReferences`.** That connection also lists open pull requests that only
  reference the issue, so it does not say which one closed it.
- **A worktree at the planned path is the ticket's.** `HostedSessionManager` looks for the branch's checkout first, as before.
  When there is none and the planned path exists, it asks `WorktreePort.isWorktreeAt`. A registered worktree there runs the
  session as it is, with nothing added, and only a directory that is not a worktree is refused. Once the head branch is
  the ticket branch, the case above is found by its checkout and needs none of this. What it serves is a ticket still on
  the slug: an issue closed by hand or by a commit, or an open ticket whose worktree has switched branch.
- **The held worktree is found the same way.** `WorktreeManager.held`, which the done ticket's worktree removal reads, takes the
  branch's checkout first and the worktree at the planned path second. Once the branch is the merged head, the planned path is
  the head's and the worktree sits at the slug's, so the path alone would find nothing to remove. The project root is never
  held, even when the branch is checked out there.
- Rejected: a second `gh` call per done ticket for its pull request, which is one request per issue (ADR 0118's reason).
  Matching the worktree by the branch it has checked out against the slug, which needs `session` to know how the slug is
  made.

## Consequences

- An issue closed by hand keeps the slug, even when its work merged: no pull request closed it.
- A worktree at the planned path with an unrelated branch checked out also runs the session. Only a directory that is not a
  worktree is refused. The user put that worktree there, and refusing it would bring back the dead end this ADR removes.
- Each issue adds one timeline item to the query. GitHub counts it against the GraphQL rate limit, but the board still reads in
  one call.
