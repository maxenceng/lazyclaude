# 42. The summary travels with the submit

Date: 2026-10-24

## Status

Accepted. Amends ADR 0009, which keeps the summary as the pending review's body, saved as it is written.

## Context

GitHub refuses to edit the body of a pending review that has none: both `updatePullRequestReview` and the REST update answer
"Could not edit a review with a missing body". A body can be given only when the review is created or when it is submitted, so
the summary cannot be saved before the submit.

## Decision

- **The summary is not written to GitHub before the submit.** `ctrl+s` in the summary box keeps the text in the review screen and
  asks `submit review as Comment? y/n` at once; `y` sends `submitPullRequestReview` with `COMMENT` and the summary as its `body`.
- **An unsubmitted summary lives only while the review screen is open.** `n` or `esc` at the confirm leaves the review pending and
  the summary typed; leaving the review or quitting loses it. A comment, by contrast, is still saved as it is written.
- **An adopted review's body is shown and edited as the summary**, and a summary typed on this screen shows in its place.
- There is no port method to set a summary: nothing keeps a write that GitHub refuses.

## Consequences

- The summary is the one thing ADR 0009's "nothing unsent is lost" does not cover.
- A summary alone can be submitted: the review is started for it if the pull request has none.
