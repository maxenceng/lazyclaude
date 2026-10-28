package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

/** Where a hosted session stands: spawned and not yet in the registry, live, or unknown because its record cannot be read. */
public sealed interface HostedPhase permits StartingPhase, LivePhase, UnreadPhase {

    /** Whether quitting would cut off work or an answer you owe: the session is busy or blocked on you. */
    boolean isBusyOrWaiting();

    /** The session as the row and the main view show it, none while it is starting. */
    Optional<LiveSession> live();

    /** This phase showing {@code session} instead; a starting one has nothing to show and stays as it is. */
    HostedPhase showing(LiveSession session);
}
