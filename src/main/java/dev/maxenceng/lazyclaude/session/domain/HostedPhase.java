package dev.maxenceng.lazyclaude.session.domain;

/** Where a hosted session stands: spawned and not yet in the registry, live, or unknown while the registry is unread. */
public sealed interface HostedPhase permits StartingPhase, LivePhase, UnreadPhase {

    /** Whether quitting would cut off work or an answer you owe: the session is busy or blocked on you. */
    boolean isBusyOrWaiting();
}
