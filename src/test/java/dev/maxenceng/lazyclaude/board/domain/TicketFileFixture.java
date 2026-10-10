package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;

import dev.maxenceng.lazyclaude.board.domain.TicketFile.TicketFileBuilder;
import dev.maxenceng.lazyclaude.board.published.FileName;

public final class TicketFileFixture {

    private TicketFileFixture() {}

    public static TicketFileBuilder ticketFileBuilder() {
        return TicketFile.builder().file(fileName()).ticket(ticket());
    }

    public static TicketFile ticketFile() {
        return ticketFileBuilder().build();
    }

    public static TicketFile ticketFileOf(Ticket ticket) {
        return ticketFileBuilder()
                .file(new FileName("%s.md".formatted(ticket.id().value())))
                .ticket(ticket)
                .build();
    }
}
