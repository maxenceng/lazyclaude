package dev.maxenceng.lazyclaude.board.published;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's id: the number GitHub gave its issue; orders numerically, so 2 precedes 10. */
public record TicketId(String value) implements Comparable<TicketId> {

    private static final Pattern SHAPE = Pattern.compile("[1-9]\\d{0,17}");
    private static final String SHAPE_IN_WORDS = "an issue number";

    public TicketId {
        Assert.matchesPattern("id", value, SHAPE, SHAPE_IN_WORDS);
    }

    @Override
    public int compareTo(TicketId other) {
        return Long.compare(Long.parseLong(value), Long.parseLong(other.value));
    }
}
