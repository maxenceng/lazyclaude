package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** What the shell asks of `board`: the board of a working directory, as a view. It only reads. */
public final class BoardQuery {

    private final BoardApplicationService boards;

    public BoardQuery(BoardApplicationService boards) {
        this.boards = boards;
    }

    /** @throws DomainException when there is no board to show, saying why */
    public BoardView boardFor(BoardPayload payload) {
        return BoardView.from(boards.boardFor(payload.toDomain(), payload.merged()));
    }

    /**
     * The board read again, or nothing when it cannot be, so a board already on screen stays.
     */
    // deferred: why a board cannot be read again is dropped, not shown — close it when a board stale for that reason is mistaken for a current one.
    public Optional<BoardView> refreshedBoardFor(BoardPayload payload) {
        try {
            return Optional.of(boardFor(payload));
        } catch (DomainException unreadable) {
            return Optional.empty();
        }
    }
}
