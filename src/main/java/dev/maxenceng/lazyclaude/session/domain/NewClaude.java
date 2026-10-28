package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** {@code claude --session-id <id>}: a plain {@code claude}, which begins a session of its own under an id lazyclaude chose. */
public record NewClaude(SessionId session) implements LaunchCommand {

    public NewClaude {
        Assert.notNull("session", session);
    }

    @Override
    public Optional<TicketRun> launchRun() {
        return Optional.empty();
    }
}
