package dev.maxenceng.lazyclaude.board.domain;

import java.time.Instant;
import java.util.Comparator;

/** When a ticket's issue was closed, or that it never was. */
public sealed interface Closure permits ClosedAt, NotClosed {

    /** The latest closed first, then what never was. */
    static Comparator<Closure> latestFirst() {
        return Comparator.comparing(
                (Closure closure) -> closure instanceof ClosedAt(Instant at) ? at : null, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
