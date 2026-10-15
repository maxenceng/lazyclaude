package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/**
 * What {@link SessionQuery} is asked a verb choice with: the project's sessions as they are asked for, the ticket, the verbs its
 * board status allows, and whether review's last read has its branch among the merged pull requests' heads.
 */
public record VerbChoicePayload(SessionPayload sessions, LinkedTicketId ticket, List<Verb> verbs, boolean merged) {

    public VerbChoicePayload {
        Assert.notNull("sessions", sessions);
        Assert.notNull("ticket", ticket);
        Assert.notEmpty("verbs", verbs);
    }
}
