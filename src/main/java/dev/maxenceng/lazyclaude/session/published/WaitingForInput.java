package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The session is blocked on you for any reason other than a permission prompt. */
public record WaitingForInput(WaitingFor reason) implements SessionState {

    public WaitingForInput {
        Assert.notNull("reason", reason);
    }
}
