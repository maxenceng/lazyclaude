package dev.maxenceng.lazyclaude.board.application;

import dev.maxenceng.lazyclaude.board.domain.Backlog;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a project's board. */
public final class ReadBoard {

    private final Backlog backlog;

    public ReadBoard(Backlog backlog) {
        this.backlog = backlog;
    }

    public Board read(Project project) {
        return backlog.read(project);
    }
}
