package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;

/** What {@link HostedSessionCommand} is asked to feed a hosted session with: a key, a paste or a mouse event. */
public record HostedInputPayload(SessionId session, TerminalInput input) {

    public HostedInputPayload {
        Assert.notNull("hosted session id", session);
        Assert.notNull("input", input);
    }
}
