package dev.maxenceng.lazyclaude.shared.domain;

public final class BranchNameFixture {

    private BranchNameFixture() {}

    public static BranchName branchName() {
        return new BranchName("PROJ-1-ticket");
    }

    public static BranchName secondBranchName() {
        return new BranchName("PROJ-2-other-ticket");
    }
}
