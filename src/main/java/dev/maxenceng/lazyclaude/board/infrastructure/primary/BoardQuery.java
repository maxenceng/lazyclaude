package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;

/** What the shell asks of `board`: the board of a working directory, as a view. It only reads. */
public final class BoardQuery {

    private final BoardApplicationService boards;

    public BoardQuery(BoardApplicationService boards) {
        this.boards = boards;
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when there is no board to show, saying why */
    public BoardView boardFor(BoardPayload payload) {
        return BoardView.from(boards.boardFor(payload.toDomain(), payload.merged()));
    }

    /**
     * The board read again, or nothing when it cannot be, so a board already on screen stays; a read that fails after a good one
     * raises a warning saying why.
     */
    public Optional<BoardView> boardReadAgainFor(BoardPayload payload) {
        return boards.boardReadAgainFor(payload.toDomain(), payload.merged()).map(BoardView::from);
    }

    /**
     * Runs {@code changed} after each batch of changes to the backlog, the ticket worktrees' copies and the refs, on a thread of the
     * watcher's own. A call does not say what changed.
     */
    public void subscribe(BoardWatchPayload payload, Runnable changed) {
        boards.subscribe(payload.toDomain(), changed);
    }
}
