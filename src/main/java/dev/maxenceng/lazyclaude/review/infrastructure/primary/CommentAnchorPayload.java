package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;

/** What {@link ReviewQuery} is asked where a comment would hang with: the diff, and the first and the last diff line of the range. */
public record CommentAnchorPayload(PullRequestDiffView diff, DiffLineIndex first, DiffLineIndex last) {}
