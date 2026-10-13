package dev.maxenceng.lazyclaude.review.published;

public final class HeadBranchFixture {

    private HeadBranchFixture() {}

    public static HeadBranch headBranch() {
        return new HeadBranch("PROJ-1-first-slice");
    }

    public static HeadBranch siblingHeadBranch() {
        return new HeadBranch("PROJ-2");
    }

    public static HeadBranch sharingTheIdPrefix() {
        return new HeadBranch("PROJ-10-another-ticket");
    }

    public static HeadBranch bareId() {
        return new HeadBranch("PROJ-1");
    }

    public static HeadBranch longerThanTheBranch() {
        return new HeadBranch("PROJ-1-first-slice-again");
    }
}
