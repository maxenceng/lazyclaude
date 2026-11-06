package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.StatusVerbs;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The verbs each status offers: those {@code config.yml} gave it, and for a status it leaves out, those {@code /ticket} accepts for it. */
public record OfferedVerbs(List<StatusVerbs> configured) {

    public OfferedVerbs {
        Assert.notNull("configured", configured);
    }

    /** The verbs each status offers when {@code config.yml} names none. */
    public static OfferedVerbs byDefault() {
        return new OfferedVerbs(List.of());
    }

    public List<Verb> of(Status status) {
        return configured.stream()
                .filter(given -> given.status() == status)
                .findFirst()
                .map(StatusVerbs::verbs)
                .orElseGet(status::verbs);
    }
}
