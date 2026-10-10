package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A project's tickets, one group per status in lifecycle order, plus the backlog files that could not be read. */
public record Board(List<StatusGroup> groups, List<UnreadableTicket> unreadable) {

    public Board {
        Assert.notNull("groups", groups);
        Assert.notNull("unreadable", unreadable);
        groups = List.copyOf(groups);
        unreadable = List.copyOf(unreadable);
    }
}
