package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.UnreadChanges;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class BoardQueryFixture {

    private BoardQueryFixture() {}

    public static BoardQuery boardQuery(ProjectLocatorPort locator, BacklogPort backlog) {
        return boardQuery(locator, backlog, unreadChanges());
    }

    public static BoardQuery boardQuery(ProjectLocatorPort locator, BacklogPort backlog, BranchChangesPort changes) {
        return boardQuery(locator, backlog, changes, mock(BoardWatchPort.class), mock(WarningPort.class));
    }

    public static BoardQuery boardQuery(
            ProjectLocatorPort locator,
            BacklogPort backlog,
            BranchChangesPort changes,
            BoardWatchPort watch,
            WarningPort warnings) {
        return new BoardQuery(new BoardApplicationService(locator, backlog, changes, watch, warnings));
    }

    public static BranchChangesPort unreadChanges() {
        return mock(BranchChangesPort.class, invocation -> new UnreadChanges());
    }
}
