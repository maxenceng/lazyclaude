package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.published.BodyFixture.secondBody;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;

import dev.maxenceng.lazyclaude.board.domain.TicketFile.TicketFileBuilder;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Status;

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

    public static TicketFile doneTicketFile() {
        return ticketFileBuilder().ticket(ticketBuilder().status(Status.DONE).build()).build();
    }

    public static TicketFile inReviewTicketFile() {
        return ticketFileBuilder()
                .ticket(ticketBuilder().status(Status.IN_REVIEW).body(secondBody()).build())
                .build();
    }
}
