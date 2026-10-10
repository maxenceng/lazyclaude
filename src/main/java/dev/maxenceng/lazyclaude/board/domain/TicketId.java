package dev.maxenceng.lazyclaude.board.domain;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A ticket's id, `PREFIX-number`; orders by prefix, then numerically, so PROJ-2 precedes PROJ-10. */
public record TicketId(String value) implements Comparable<TicketId> {

    private static final Pattern SHAPE = Pattern.compile("([A-Za-z][A-Za-z0-9]*)-(\\d{1,18})");
    private static final Comparator<TicketId> ORDER =
            Comparator.comparing(TicketId::prefix).thenComparingLong(TicketId::number);

    public TicketId {
        if (value == null || !SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("id '" + value + "' is not PREFIX-number");
        }
    }

    public static TicketId of(String value) {
        return new TicketId(value);
    }

    private String prefix() {
        return matcher().group(1);
    }

    private long number() {
        return Long.parseLong(matcher().group(2));
    }

    private Matcher matcher() {
        Matcher matcher = SHAPE.matcher(value);
        matcher.matches();
        return matcher;
    }

    @Override
    public int compareTo(TicketId other) {
        return ORDER.compare(this, other);
    }
}
