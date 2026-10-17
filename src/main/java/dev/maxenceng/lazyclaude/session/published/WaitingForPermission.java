package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The session is blocked on a permission prompt, with the reason Claude Code gives for it. */
public record WaitingForPermission(WaitingReason reason) implements SessionState {

    public WaitingForPermission {
        Assert.notNull("reason", reason);
    }
}
