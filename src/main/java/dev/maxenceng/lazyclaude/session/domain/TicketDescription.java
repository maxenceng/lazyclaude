package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a ticket about to be created is described as, typed by the person and handed to {@code /<plugin>:ticket new} as typed. */
public record TicketDescription(String value) {

    public TicketDescription {
        Assert.notBlank("ticket description", value);
    }
}
