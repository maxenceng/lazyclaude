package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoardAgain;
import static org.mockito.Mockito.when;

import java.util.Optional;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;

final class BoardRead {

    private BoardRead() {}

    static void returning(BoardQuery boards, BoardReadingView board) {
        when(boards.boardAfterChange(readingBoardAgain())).thenReturn(Optional.of(board));
    }
}
