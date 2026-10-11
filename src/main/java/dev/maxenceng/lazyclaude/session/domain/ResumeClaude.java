package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** {@code claude --resume} of a session. */
public record ResumeClaude(SessionId session) implements LaunchCommand {

    public ResumeClaude {
        Assert.notNull("session", session);
    }
}
