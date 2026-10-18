package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;

/** What {@link PendingReviewCommand} is asked a deletion with: the project, the pull request and the pending comment. */
public record PendingDeletePayload(Path root, PullRequestNumber number, PendingCommentId comment) {}
