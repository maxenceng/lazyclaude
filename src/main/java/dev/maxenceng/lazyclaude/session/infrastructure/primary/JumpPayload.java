package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** What {@link OutsideSessionCommand} is asked a jump with: the project and its ticket ids, as for any read of the sessions, and the session to bring forward. */
public record JumpPayload(SessionPayload sessions, SessionId session) {

    public JumpPayload {
        Assert.notNull("sessions", sessions);
        Assert.notNull("session", session);
    }
}
