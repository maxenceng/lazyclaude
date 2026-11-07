package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The hosted session's record is there and cannot be used, so what the session is doing is unknown. */
public record UnreadPhase(LiveSession session) implements HostedPhase {

    public UnreadPhase {
        Assert.notNull("session", session);
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
