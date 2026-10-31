package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.BoardLookup;
import dev.maxenceng.lazyclaude.board.domain.BoardReading;
import dev.maxenceng.lazyclaude.board.domain.NoProject;

/** What asking for a working directory's board answers: the project's board as the shell reads it, or why there is no project. */
public sealed interface BoardLookupView permits BoardReadingView, NoProjectView {

    static BoardLookupView from(BoardLookup lookup) {
        return switch (lookup) {
            case BoardReading reading -> BoardReadingView.from(reading);
            case NoProject none -> NoProjectView.from(none);
        };
    }
}
