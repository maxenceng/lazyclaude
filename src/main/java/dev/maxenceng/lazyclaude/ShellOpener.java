package dev.maxenceng.lazyclaude;

import dev.maxenceng.lazyclaude.board.published.BoardView;

/** Opens the shell on a board; a seam so the start-up can be tested without a terminal. */
@FunctionalInterface
interface ShellOpener {

    void open(BoardView board) throws Exception;
}
