package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.review.published.Summary;

/** No summary typed on the review screen since it opened. */
public record UntypedSummaryPayload() implements SummaryDraftPayload {

    @Override
    public Optional<Summary> typed() {
        return Optional.empty();
    }
}
