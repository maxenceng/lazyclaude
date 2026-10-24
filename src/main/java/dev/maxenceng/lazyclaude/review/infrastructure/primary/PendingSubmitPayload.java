package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;

/** What {@link PendingReviewCommand} is asked a submit with: the project, the pull request and the summary that travels with it as the review's body. */
public record PendingSubmitPayload(Path root, PullRequestNumber number, Summary summary) {}
