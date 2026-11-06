package dev.maxenceng.lazyclaude.board.domain;

import java.time.Instant;
import java.util.Comparator;

/** When a ticket's issue was closed, or that it never was. */
public sealed interface Closure permits ClosedAt, NotClosed {

    /** What never was closed first, as a done ticket left open was marked done just now, then the latest closed. */
    static Comparator<Closure> latestFirst() {
        return Comparator.comparing(
                (Closure closure) -> closure instanceof ClosedAt(Instant at) ? at : null, Comparator.nullsFirst(Comparator.reverseOrder()));
    }
}
