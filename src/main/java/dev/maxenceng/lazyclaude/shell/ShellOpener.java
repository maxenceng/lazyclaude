package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;

/** Opens the shell on a board, and the docs tree and live sessions of its project; a seam so the start-up can be tested without a terminal. */
@FunctionalInterface
interface ShellOpener {

    void open(BoardReadingView board, DocsView docs, SessionsView sessions) throws Exception;
}
