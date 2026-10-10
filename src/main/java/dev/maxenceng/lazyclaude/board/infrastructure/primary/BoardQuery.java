package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;

/** What the shell asks of `board`: the board of a working directory, as a view. It only reads. */
public final class BoardQuery {

    private final BoardApplicationService boards;

    public BoardQuery(BoardApplicationService boards) {
        this.boards = boards;
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when there is no board to show, saying why */
    public BoardView boardFor(BoardPayload payload) {
        return BoardView.from(boards.boardFor(payload.toDomain()));
    }
}
