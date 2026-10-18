package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;

/** What {@link PendingReviewCommand} is asked an edit with: the project, the pull request, the pending comment and what it now says; empty deletes it. */
public record PendingEditPayload(Path root, PullRequestNumber number, PendingCommentId comment, CommentBody body) {}
