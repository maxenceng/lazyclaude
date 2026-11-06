package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.review.published.Summary;

/** The summary as the review screen holds it: one typed there, or none typed yet. */
public sealed interface SummaryDraftPayload permits TypedSummaryPayload, UntypedSummaryPayload {

    Optional<Summary> typed();
}
