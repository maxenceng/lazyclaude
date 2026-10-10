package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.StatusGroup;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.domain.WorkingDirectory;
import dev.maxenceng.lazyclaude.board.published.BoardView;
import dev.maxenceng.lazyclaude.board.published.StatusGroupView;
import dev.maxenceng.lazyclaude.board.published.TicketView;
import dev.maxenceng.lazyclaude.board.published.UnreadableTicketView;

/** What the shell asks of `board`: it answers in published views, so the shell never sees the domain. */
public final class BoardController {

    private final BoardApplicationService boards;

    public BoardController(BoardApplicationService boards) {
        this.boards = boards;
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when there is no board to show, saying why */
    public BoardView boardFor(Path from) {
        Board board = boards.boardFor(new WorkingDirectory(from));
        return new BoardView(
                board.groups().stream().map(BoardController::view).toList(),
                board.unreadable().stream().map(BoardController::view).toList());
    }

    private static StatusGroupView view(StatusGroup group) {
        return new StatusGroupView(
                group.status().label(),
                group.status().finished(),
                group.tickets().stream().map(BoardController::view).toList());
    }

    private static TicketView view(Ticket ticket) {
        return new TicketView(
                ticket.id().value(), ticket.title(), ticket.status().label(), ticket.context(), ticket.type());
    }

    private static UnreadableTicketView view(UnreadableTicket unreadable) {
        return new UnreadableTicketView(unreadable.file(), unreadable.reason());
    }
}
