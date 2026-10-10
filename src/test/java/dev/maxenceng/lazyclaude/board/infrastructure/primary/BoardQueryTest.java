package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.ContextName;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;
import dev.maxenceng.lazyclaude.board.published.TypeName;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BoardQueryTest {

    private static BoardView boardOf(BacklogEntry... entries) {
        return new BoardQuery(new BoardApplicationService(from -> new Project(from.path()), project -> List.of(entries)))
                .boardFor(new BoardPayload(Path.of("/work/app")));
    }

    @Test
    void shouldViewEveryStatusGroupInLifecycleOrder() {
        assertThat(boardOf().groups()).extracting(StatusGroupView::status).containsExactly(Status.values());
    }

    @Test
    void shouldViewATicketsFields() {
        BoardView board = boardOf(ticketFile("A-1.md", "A-1", Status.IN_REVIEW));

        assertThat(board.groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(new TicketView(
                        new TicketId("A-1"),
                        new Title("Ticket A-1"),
                        Status.IN_REVIEW,
                        new ContextName("board"),
                        new TypeName("feature")));
    }

    @Test
    void shouldViewAnUnreadableFileWithItsReason() {
        UnreadableTicket unreadable = UnreadableTicket.builder()
                .file(new FileName("A-2.md"))
                .reason(new Reason("status is missing"))
                .build();

        assertThat(boardOf(unreadable).unreadable())
                .containsExactly(new UnreadableTicketView(new FileName("A-2.md"), new Reason("status is missing")));
    }
}
