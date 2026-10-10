package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.Status;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.TicketId;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.BoardView;
import dev.maxenceng.lazyclaude.board.published.StatusGroupView;
import dev.maxenceng.lazyclaude.board.published.TicketView;
import dev.maxenceng.lazyclaude.board.published.UnreadableTicketView;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BoardControllerTest {

    private static BoardView boardOf(BacklogEntry... entries) {
        return new BoardController(new BoardApplicationService(
                        from -> new Project(from.path()), project -> List.of(entries)))
                .boardFor(Path.of("/work/app"));
    }

    @Test
    void shouldPublishEveryStatusGroupInLifecycleOrder() {
        assertThat(boardOf().groups())
                .extracting(StatusGroupView::status)
                .containsExactly("draft", "todo", "in-progress", "in-review", "done");
    }

    @Test
    void shouldMarkOnlyTheDoneGroupAsFinished() {
        assertThat(boardOf().groups()).filteredOn(StatusGroupView::finished).extracting(StatusGroupView::status)
                .containsExactly("done");
    }

    @Test
    void shouldPublishATicketsFields() {
        BoardView board = boardOf(new TicketFile(
                "A-1.md",
                Ticket.builder()
                        .id(new TicketId("A-1"))
                        .title("A board")
                        .status(Status.IN_REVIEW)
                        .context("board")
                        .type("feature")
                        .build()));

        assertThat(board.groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .containsExactly(new TicketView("A-1", "A board", "in-review", "board", "feature"));
    }

    @Test
    void shouldPublishAnUnreadableFileWithItsReason() {
        assertThat(boardOf(new UnreadableTicket("A-2.md", "status is missing")).unreadable())
                .containsExactly(new UnreadableTicketView("A-2.md", "status is missing"));
    }
}
