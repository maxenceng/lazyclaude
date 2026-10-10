package dev.maxenceng.lazyclaude.board.application;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardManager;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.board.domain.WorkingDirectory;

/** Reads the board of the project a working directory belongs to. */
public final class BoardApplicationService {

    private final BoardManager manager;

    public BoardApplicationService(ProjectLocatorPort locator, BacklogPort backlog) {
        this.manager = new BoardManager(locator, backlog);
    }

    public Board boardFor(WorkingDirectory from) {
        return manager.boardFor(from);
    }
}
