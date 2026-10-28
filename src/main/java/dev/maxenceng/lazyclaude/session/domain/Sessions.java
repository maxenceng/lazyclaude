package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** What a project's sessions are as the Sessions panel shows them: the live and past ones, or the reason none could be read. */
public sealed interface Sessions permits AvailableSessions, UnavailableSessions {

    /** These sessions with {@code open} laid over them: only the live ones lazyclaude spawned are kept, and each has a tab. */
    Sessions hosting(HostedSessions open);

    /** Whether a session last ran {@code verb} on {@code ticket}, or lazyclaude started one to. */
    // deferred: a session counts by its last verb on the ticket, so one that ran design and then review reads as no design run — close it when a design run is missed for that.
    boolean ranVerb(LinkedTicketId ticket, Verb verb);
}
