package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Summary;

/** A summary typed on the review screen, empty included: an emptied box is a summary of nothing, not one left untyped. */
public record TypedSummaryPayload(Summary summary) implements SummaryDraftPayload {

    public TypedSummaryPayload {
        Assert.notNull("summary", summary);
    }

    @Override
    public Optional<Summary> typed() {
        return Optional.of(summary);
    }
}
