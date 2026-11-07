package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The registry holds a record for the hosted session's pid: this is the live session that record describes. */
public record LivePhase(LiveSession session) implements HostedPhase {

    public LivePhase {
        Assert.notNull("session", session);
    }

    @Override
    public Optional<LiveSession> live() {
        return Optional.of(session);
    }

    @Override
    public HostedPhase showing(LiveSession other) {
        return new LivePhase(other);
    }
}
