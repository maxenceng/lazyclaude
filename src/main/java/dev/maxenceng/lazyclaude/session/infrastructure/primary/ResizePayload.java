package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.PaneSize;

/** What {@link HostedSessionCommand} is asked to resize a hosted session's pty with. */
public record ResizePayload(HostedSessionId session, PaneSize size) {

    public ResizePayload {
        Assert.notNull("hosted session id", session);
        Assert.notNull("pane size", size);
    }
}
