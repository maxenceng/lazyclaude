package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The id of a ticket a session is linked to, spelt as the board spells it. */
public record LinkedTicketId(String value) {

    public LinkedTicketId {
        Assert.notBlank("ticket id", value);
    }
}
