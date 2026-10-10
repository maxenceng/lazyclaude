package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The tickets of one status, in id order. */
public record StatusGroup(Status status, List<Ticket> tickets) {

    public StatusGroup {
        Assert.notNull("status", status);
        Assert.notNull("tickets", tickets);
        tickets = List.copyOf(tickets);
    }
}
