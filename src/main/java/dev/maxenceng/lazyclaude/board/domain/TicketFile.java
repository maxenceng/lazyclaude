package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A backlog file read into a ticket; the file is kept so a second claim on its id can be named. */
public record TicketFile(FileName file, Ticket ticket) implements BacklogEntry {

    public TicketFile {
        Assert.notNull("file", file);
        Assert.notNull("ticket", ticket);
    }

    public static TicketFileBuilder builder() {
        return new TicketFileBuilder();
    }

    public static final class TicketFileBuilder {
        private FileName file;
        private Ticket ticket;

        private TicketFileBuilder() {}

        public TicketFileBuilder file(FileName file) {
            this.file = file;
            return this;
        }

        public TicketFileBuilder ticket(Ticket ticket) {
            this.ticket = ticket;
            return this;
        }

        public TicketFile build() {
            return new TicketFile(file, ticket);
        }
    }
}
