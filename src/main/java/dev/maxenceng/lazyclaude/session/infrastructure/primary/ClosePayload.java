package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link HostedSessionCommand} is asked to close with: the hosted session whose {@code claude} should end. */
public record ClosePayload(SessionId session) {

    public ClosePayload {
        Assert.notNull("hosted session id", session);
    }
}
