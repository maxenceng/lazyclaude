package dev.maxenceng.lazyclaude;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;

/** Opens the shell on a board and the docs tree of its project; a seam so the start-up can be tested without a terminal. */
@FunctionalInterface
interface ShellOpener {

    void open(BoardView board, DocsView docs) throws Exception;
}
