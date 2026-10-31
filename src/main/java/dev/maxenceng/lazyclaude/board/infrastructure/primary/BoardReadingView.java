package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardReading;
import dev.maxenceng.lazyclaude.board.domain.UnavailableBoard;
import dev.maxenceng.lazyclaude.board.published.ProjectRoot;

/** A project's board as the shell reads it: its tickets, or why its issues could not be read; either way the root it was read from. */
public sealed interface BoardReadingView extends BoardLookupView permits BoardView, UnavailableBoardView {

    ProjectRoot root();

    static BoardReadingView from(BoardReading reading) {
        return switch (reading) {
            case Board board -> BoardView.from(board);
            case UnavailableBoard unavailable -> UnavailableBoardView.from(unavailable);
        };
    }
}
