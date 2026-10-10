package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocatorPort;

public final class BoardQueryFixture {

    private BoardQueryFixture() {}

    public static BoardQuery boardQuery(ProjectLocatorPort locator, BacklogPort backlog) {
        return new BoardQuery(new BoardApplicationService(locator, backlog));
    }
}
