package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;

public final class BoardQueryFixture {

    private BoardQueryFixture() {}

    public static BoardQuery boardQuery(ProjectLocatorPort locator, BacklogPort backlog) {
        return boardQuery(locator, backlog, mock(BranchCopyPort.class));
    }

    public static BoardQuery boardQuery(ProjectLocatorPort locator, BacklogPort backlog, BranchCopyPort copies) {
        return new BoardQuery(new BoardApplicationService(locator, backlog, copies));
    }
}
