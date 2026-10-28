package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What the hosted {@code claude} is asked to do: begin a session, pick one up again, or run {@code /ticket} on a ticket. */
public sealed interface LaunchCommand permits NewClaude, ResumeClaude, TicketClaude {

    /** The session the {@code claude} runs as: named when it is begun, the one resumed when it is picked up again. */
    SessionId session();

    /** The {@code /ticket} run this launch is, none for a plain or resumed one. */
    Optional<TicketRun> launchRun();
}
