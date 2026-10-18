package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.ThreadId;

/** What {@link PendingReviewCommand} is asked a reply with: the project, the pull request, the thread answered and what the reply says. */
public record PendingReplyPayload(Path root, PullRequestNumber number, ThreadId thread, CommentBody body) {}
