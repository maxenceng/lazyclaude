# 90. `review` decides the review screen's rules both frontends show

Date: 2026-11-06

## Status

Accepted. Amends ADR 0076: `threadsInDiff` and `pendingInDiff` become one `commentsInDiff`. Amends ADR 0089: the screen
rules it ported from the shell move into `review`. Departs from the desktop Review screen ticket's model decision, which
allowed no backend change.

## Context

ADR 0089 left five rules of the review screen in both the shell and `frontend/src/review/`: what a submit sends (the summary
typed, else the adopted review's body), resolved threads kept out of the diff, pending comments listed before threads in
Comments, the pending comment `enter` and `x` act on when a line holds several (the one written last), and the join of a
ticket's branch to its open pull request. Two copies of a rule drift apart, and nothing fails when they do.

## Decision

- **`ReviewQuery.commentsInDiff`**, served as `review/commentsInDiff`, takes the diff, the threads and the pending review as
  each read last answered, and whether resolved threads are shown. It answers every placed thread and pending comment, the
  threads that show under their lines, what Comments lists in order, and per line the pending comment written last. The
  rules are `CommentsInDiff` in the domain. It replaces `threadsInDiff` and `pendingInDiff` on the query and the protocol.
- **`ReviewQuery.summaryToSubmit`**, served as `review/summaryToSubmit`, takes the pending review and the summary typed,
  `TypedSummaryPayload | UntypedSummaryPayload`. An emptied box is a typed summary of nothing, not one left untyped. The rule
  is `PendingReviewManager.summaryToSubmit`.
- **`ReviewQuery.pullRequestOn`**, served as `review/pullRequestOn`, takes the pull requests as last pushed and a branch,
  and answers `HeadedPullRequestView | NoHeadedPullRequestView` from `OpenPullRequestsView.on`, which the shell already reads.
- Both frontends call these operations and keep no copy of the rules. The shell's `CommentEntry` gives way to
  `CommentInDiffView`, which `ThreadInDiffView` and `PendingInDiffView` now implement, so each carries a `kind` on the wire.

## Consequences

- The window waits one round trip more for what used to be worked out in place: `r` asks again before resolved threads show
  or hide, `S` before the summary box opens, `y` before the submit goes out, and `p` before the review opens. Each ask's
  failure is a notice, or the diff's fault with its retry for `commentsInDiff`.
- Every read that lands asks `commentsInDiff` again, even before the diff is read, since `review` now answers the unread
  cases too; the window redraws the read's own state first, so the diff shows before its comments do.
- The window still numbers the diff lines itself (ADR 0089's `deferred:`); the indexes `commentsInDiff` answers are the
  ones it counts against.
