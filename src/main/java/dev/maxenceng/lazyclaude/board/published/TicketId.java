package dev.maxenceng.lazyclaude.board.published;

import java.util.Comparator;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket's id, `PREFIX-number`; orders by prefix, then numerically, so PROJ-2 precedes PROJ-10. */
public record TicketId(String value) implements Comparable<TicketId> {

    private static final Pattern SHAPE = Pattern.compile("[A-Za-z][A-Za-z0-9]*-\\d{1,18}");
    private static final String SHAPE_IN_WORDS = "PREFIX-number";
    private static final char SEPARATOR = '-';
    private static final Comparator<TicketId> ORDER =
            Comparator.comparing(TicketId::prefix).thenComparingLong(TicketId::number);

    public TicketId {
        Assert.matchesPattern("id", value, SHAPE, SHAPE_IN_WORDS);
    }

    private String prefix() {
        return value.substring(0, value.indexOf(SEPARATOR));
    }

    private long number() {
        return Long.parseLong(value.substring(value.indexOf(SEPARATOR) + 1));
    }

    @Override
    public int compareTo(TicketId other) {
        return ORDER.compare(this, other);
    }
}
