package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;

/** What the shell asks of `board`: the board of a working directory, as a view. It only reads. */
public final class BoardQuery {

    private final BoardApplicationService boards;

    public BoardQuery(BoardApplicationService boards) {
        this.boards = boards;
    }

    /**
     * The board of the project, or a view without tickets that says why its issues cannot be read.
     *
     * @throws dev.maxenceng.lazyclaude.error.domain.DomainException when the directory belongs to no project
     */
    public BoardView boardFor(BoardPayload payload) {
        return BoardView.from(boards.boardFor(payload.toDomain()));
    }

    /**
     * The board read again, or nothing when it cannot be, so a board already on screen stays; a read that fails after a good one
     * raises a warning saying why.
     */
    public Optional<BoardView> boardReadAgainFor(BoardPayload payload) {
        return boards.boardReadAgainFor(payload.toDomain()).map(BoardView::from);
    }

    /**
     * Runs {@code changed} on a thread of the watcher's own whenever the project's issues changed, or could not be checked. A call
     * does not say what changed.
     */
    public void subscribe(BoardWatchPayload payload, Runnable changed) {
        boards.subscribe(payload.toDomain(), changed);
    }
}
