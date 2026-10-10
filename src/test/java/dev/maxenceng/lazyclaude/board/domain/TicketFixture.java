package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;

public final class TicketFixture {

    private TicketFixture() {}

    public static Ticket ticket(String id, Status status) {
        return Ticket.builder()
                .id(new TicketId(id))
                .title(new Title("Ticket %s".formatted(id)))
                .status(status)
                .context(new ContextName("board"))
                .type(new TypeName("feature"))
                .build();
    }

    public static TicketFile ticketFile(String file, String id, Status status) {
        return TicketFile.builder().file(new FileName(file)).ticket(ticket(id, status)).build();
    }
}
