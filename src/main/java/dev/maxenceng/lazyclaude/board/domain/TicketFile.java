package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A backlog file read into a ticket; the file is kept so a second claim on its id can be named. */
public record TicketFile(String file, Ticket ticket) implements BacklogEntry {

    public TicketFile {
        Assert.notNull("file", file);
        Assert.notNull("ticket", ticket);
    }
}
