package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.published.TicketBranch;

public final class CheckedOutBranchFixture {

    private CheckedOutBranchFixture() {}

    public static CheckedOutBranch checkedOut(TicketBranch branch) {
        return new CheckedOutBranch(branch.value());
    }

    public static CheckedOutBranch trunk() {
        return new CheckedOutBranch("main");
    }

    public static CheckedOutBranch closeBranch() {
        return new CheckedOutBranch("close-PROJ-1");
    }
}
