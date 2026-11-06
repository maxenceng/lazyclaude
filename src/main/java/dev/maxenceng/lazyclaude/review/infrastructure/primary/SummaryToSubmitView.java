package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.published.Summary;

/** What the summary box starts from and a submit sends. */
public record SummaryToSubmitView(Summary summary) {}
