package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link HostedSessionCommand} is asked to have repainted with: the hosted session whose {@code claude} should draw its whole screen again. */
public record RepaintPayload(SessionId session) {

    public RepaintPayload {
        Assert.notNull("hosted session id", session);
    }
}
