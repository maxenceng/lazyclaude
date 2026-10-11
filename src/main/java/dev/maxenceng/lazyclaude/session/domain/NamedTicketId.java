package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket id as a transcript spelt it, in whatever case, before the board says whether it has such a ticket. */
public record NamedTicketId(String value) {

    public NamedTicketId {
        Assert.notBlank("named ticket id", value);
    }
}
