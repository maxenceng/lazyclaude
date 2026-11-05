package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.MergeState;

/**
 * What {@link SessionQuery} is asked a new session from a ticket with: the project's sessions as they are asked for, the ticket, the
 * verbs its board status allows, none included, and whether its branch is among the merged pull requests' heads, as far as the
 * asker has read them.
 */
public record NewSessionPayload(SessionPayload sessions, LinkedTicketId ticket, List<Verb> verbs, MergeState merged) {

    public NewSessionPayload {
        Assert.notNull("sessions", sessions);
        Assert.notNull("ticket", ticket);
        Assert.notNull("verbs", verbs);
        Assert.notNull("merge state", merged);
    }
}
