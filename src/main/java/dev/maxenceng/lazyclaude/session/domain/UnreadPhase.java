package dev.maxenceng.lazyclaude.session.domain;

/**
 * The registry could not be read, so whether the hosted session is starting, busy or waiting on you is unknown. It counts
 * as busy: ending a {@code claude} mid-work cannot be undone, so quitting asks rather than guesses.
 */
public record UnreadPhase() implements HostedPhase {

    @Override
    public boolean isBusyOrWaiting() {
        return true;
    }
}
