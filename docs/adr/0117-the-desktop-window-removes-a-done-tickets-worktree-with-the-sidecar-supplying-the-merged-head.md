# 117. The desktop window removes a done ticket's worktree, with the sidecar supplying the merged head

Date: 2026-11-19

## Status

Accepted. Applies ADR 0038 (the worktree is judged again at the yes, the join stays in the composer) and ADR 0030 (the ticket
worktree lives in `session`) to the desktop window; the window's popup is ADR 0109's. Amends ADR 0050 by three methods and one
notification.

## Context

The terminal removes a done ticket's worktree with `d`, or asks once a `/ticket done` session ends. The window did not: `d` was
a declared, unhandled action. The shell joins what `session` cannot read for itself, the project's sessions and the commit
`review` last read the branch's pull request merged at, into a `WorktreePayload`. A window that sent those would carry merge data
it never reads and could send a stale one.

## Decision

- **The window asks with the ticket's branch alone.** `session/worktreeOf` and `session/removeWorktree` take
  `WorktreeBranchPayload`. The sidecar composes the `WorktreePayload` as the shell does: the `SessionPayload` of the last
  `session/watch`, and the merged head from the last `review/mergedChanged` it pushed (`MergedPullRequestsView.mergedHeadOf`). The
  join stays in the composer, so `session` never reads `review` or `board`. A branch with no merged pull request, or read before
  `review` has answered, has no merged head, so its state is `noMergedPullRequest` and nothing is offered for removal.
- **A result of its own, with the words in it.** `session/worktreeOf` answers `WorktreeShownView`: `noWorktree`, `clearWorktree`
  (`shown`), `heldBackWorktree` (`shown`, `state`, `refusal`) or `unreadWorktree` (`reason`, `refusal`). `shown` is the path from
  the project's parent, as the shell shows it. The window draws a `clean` pill for `clearWorktree`, `held` for a `heldBackWorktree`
  whose state is `worktreeContents`, and `unknown` for one whose state is `noMergedPullRequest` and for `unreadWorktree`.
  `session/removeWorktree` answers `WorktreeRemovalOutcomeView`: `removedWorktree` or `notRemovedWorktree` (`refusal`). A refusal
  and a failure in git are both `notRemovedWorktree`: the window says the line and shows nothing else.
- **The words have one source.** `WorktreeWording`, package-private in `session`'s primary side, words `cannot remove: ...`; the
  views hand it out (`WorktreeStateView.refusal`, `UnreadableWorktreeView.refusal`, `FailedRemovalView.refusal`,
  `NoWorktreeView.refusal`), and the shell's `WorktreeText` reads them instead of owning a copy. `DirtyPath` moved back to
  `session.domain`: with the wording in `session`, nothing outside reads it (ADR 0029).
- **Refusal is a result.** The judgement is made again at the yes by the existing `TicketWorktreeCommand.remove`; a worktree that
  changed since shown, or a failed `git worktree remove`, comes back as `notRemovedWorktree` with its reason, never a JSON-RPC
  error. A call before any `session/watch` is also `unreadWorktree` / `notRemovedWorktree`, not an error. The local branch is kept.
- **`session/ticketDoneEnded` tells the window which ticket's `/ticket done` session ended.** It is raised from
  `SessionQuery.onTicketDoneEnded` and its params are the ticket id. Which ticket is `done` stays `board`'s answer: the window reads
  the board again, as the shell does, and opens its popup only when the ticket is `done` and its worktree reads `clearWorktree`.
- **Where `d` acts in the window.** The Base tab's ticket pane shows a worktree section after `sessions`, not the Board tab's
  preview. `d` with the Board or the ticket pane focused, or a click on the section's row, opens ADR 0109's popup `Remove this
  worktree?` with `Keep (esc)` and `Remove (y)`, and the key has a visible button. The `clean` pill has its own neutral token.
  A session ending opens the popup unasked only after a `/ticket done` session, never from a stale read.

## Consequences

- The window's `RemoveWorktree` action leaves its unhandled list once the frontend handles it.
- The sidecar serves one project (`WatchedRoot`), so one remembered `SessionPayload` and one merged read are enough. A second
  project would need them per root.
- The merged head is the last one pushed: a merge that lands between the push and the yes is judged by the older head, and the
  yes judges the worktree again against it. `review` pushing a newer one is read at the next yes.
- `session/ticketDoneEnded`'s params are a bare ticket id, not an object: a record of one component is written as its component
  (ADR 0073).
