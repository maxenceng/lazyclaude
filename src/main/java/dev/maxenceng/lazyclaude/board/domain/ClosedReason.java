package dev.maxenceng.lazyclaude.board.domain;

import java.util.Locale;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Why GitHub says an issue was closed, in its own word ({@code COMPLETED}, {@code NOT_PLANNED}); blank when it gives none. */
public record ClosedReason(String value) {

    private static final String COMPLETED = "COMPLETED";

    public ClosedReason {
        Assert.notNull("closed reason", value);
    }

    /** Closed as completed, or with no reason given: the ticket is done (ADR 0065). */
    public boolean completed() {
        return value.isBlank() || value.equals(COMPLETED);
    }

    /** The reason as a person reads it: {@code NOT_PLANNED} is {@code not planned}. */
    public String phrase() {
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
