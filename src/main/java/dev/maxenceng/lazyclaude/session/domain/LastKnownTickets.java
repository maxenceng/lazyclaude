package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** The ids of the tickets on the board as the shell last handed them over, which sessions are linked against on every read. None until it is told. */
public final class LastKnownTickets {

    private final AtomicReference<KnownTickets> current = new AtomicReference<>(new KnownTickets(List.of()));

    public void know(KnownTickets tickets) {
        current.set(tickets);
    }

    public KnownTickets current() {
        return current.get();
    }
}
