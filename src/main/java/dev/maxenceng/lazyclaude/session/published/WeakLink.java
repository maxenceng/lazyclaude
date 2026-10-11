package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Only a branch the session recorded names the ticket; shown as less certain, with no verb. */
public record WeakLink(LinkedTicketId ticket) implements TicketLink {

    public WeakLink {
        Assert.notNull("ticket", ticket);
    }
}
