package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * The hosted session's record is there and cannot be used, so whether it is busy or waiting on you is unknown. It counts as
 * busy: ending a {@code claude} mid-work cannot be undone, so quitting asks rather than guesses.
 */
public record UnreadPhase(LiveSession session) implements HostedPhase {

    public UnreadPhase {
        Assert.notNull("session", session);
    }

    @Override
    public boolean isBusyOrWaiting() {
        return true;
    }

    @Override
    public Optional<LiveSession> live() {
        return Optional.of(session);
    }

    @Override
    public HostedPhase showing(LiveSession other) {
        return new UnreadPhase(other);
    }
}
