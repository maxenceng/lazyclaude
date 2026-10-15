package dev.maxenceng.lazyclaude.board.application;

import java.util.Set;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.BoardManager;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Reads the board of the project a working directory belongs to, each ticket from the copy that stands for it. */
public final class BoardApplicationService {

    private final BoardManager manager;

    public BoardApplicationService(ProjectLocatorPort locator, BacklogPort backlog, BranchCopyPort copies) {
        this.manager = new BoardManager(locator, backlog, copies);
    }

    public Board boardFor(WorkingDirectory from, Set<TicketBranch> merged) {
        return manager.boardFor(from, merged);
    }
}
