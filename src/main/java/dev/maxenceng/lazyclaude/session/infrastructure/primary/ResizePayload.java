package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link HostedSessionCommand} is asked to resize a hosted session's pty with. */
public record ResizePayload(SessionId session, PaneSize size) {

    public ResizePayload {
        Assert.notNull("hosted session id", session);
        Assert.notNull("pane size", size);
    }
}
