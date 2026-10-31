package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link HostedSessionCommand} is asked to write to a hosted session's pty with: the session and the bytes its frontend already encoded. */
public record PtyInputPayload(SessionId session, PtyBytes data) {

    public PtyInputPayload {
        Assert.notNull("hosted session id", session);
        Assert.notNull("pty bytes", data);
    }
}
