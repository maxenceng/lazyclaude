package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;

/** What {@link PendingReviewCommand} is asked the summary with: the project, the pull request and what the summary now says. */
public record PendingSummaryPayload(Path root, PullRequestNumber number, Summary summary) {}
