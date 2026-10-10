# 9. Unsent review comments live in GitHub's pending review

Date: 2026-10-10

## Status

Accepted

## Context

lazyclaude's PR review lets the user write line comments and a summary, then submit them as
one *Comment* review. The comments are written over several minutes, and the user may leave
the review, quit lazyclaude, or lose it to a crash before submitting. The research behind the
review screen recommended composing everything locally and posting it in a single
`POST /pulls/{n}/reviews`, because that leaves no server-side draft to clean up.

A prototype compared three answers for unsent comments: keep them in memory until quit, force
a submit-or-discard choice on leaving, or save each one to a GitHub pending review as it is
written. The user chose the pending review.

## Decision

Each comment is written to the user's pending review on the PR the moment it is saved:
the first creates the review (`addPullRequestReview` with no event), later ones add threads
to it (`addPullRequestReviewThread`), and edits and deletions go to the server too. The summary
is the pending review's body. Submitting sends the `COMMENT` event to that review.

When the review screen opens on a PR that already has a pending review of the user's, from
github.com or an earlier lazyclaude, lazyclaude adopts it. GitHub allows only one per PR.
When the PR's head has moved past the commit the pending review was written against, the
review screen warns and still submits. GitHub marks comments whose lines moved as outdated.

## Consequences

- **Nothing unsent is lost** to leaving, quitting or a crash, and nothing has to be confirmed
  on quit.
- **Every save is a network call**, and the comment screen depends on GitHub being reachable
  to save, not only to submit.
- **Pending comments are visible on github.com** to the user only, and a review started there
  continues in lazyclaude.
- **Writes go through GraphQL**, not the single REST call the research recommended: REST has
  no call that adds a comment to an existing pending review.
