package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;

/** What a frontend asks of `board`: the board of a working directory, as a view. It only reads. */
public final class BoardQuery {

    private final BoardApplicationService boards;

    public BoardQuery(BoardApplicationService boards) {
        this.boards = boards;
    }

    /** The board of the project, or a view without tickets that says why its issues cannot be read, or why there is no project. */
    public BoardLookupView boardFor(BoardPayload payload) {
        return BoardLookupView.from(boards.boardFor(payload.toDomain()));
    }

    /**
     * The board read again after a change, or nothing when what is shown stays: a board with tickets is kept when the read fails, the
     * warning saying why, and one without is read whole so its reason stays current.
     */
    public Optional<BoardReadingView> boardAfterChange(BoardAfterChangePayload payload) {
        return boards.boardAfterChange(payload.toDomain(), payload.boardShown()).map(BoardReadingView::from);
    }

    /**
     * Runs {@code changed} on a thread of the watcher's own whenever the project's issues changed, or could not be checked. A call
     * does not say what changed.
     */
    public void subscribe(BoardWatchPayload payload, Runnable changed) {
        boards.subscribe(payload.toDomain(), changed);
    }
}
