package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.PaneHeight;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.PaneWidth;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link HostedSessionCommand} is asked to resize a hosted session's pty with, as a frontend that emulates it counts the pane: columns and rows. */
public record PtyResizePayload(SessionId session, PaneWidth cols, PaneHeight rows) {

    public PtyResizePayload {
        Assert.notNull("hosted session id", session);
        Assert.notNull("columns", cols);
        Assert.notNull("rows", rows);
    }

    PaneSize size() {
        return PaneSize.builder().width(cols).height(rows).build();
    }
}
