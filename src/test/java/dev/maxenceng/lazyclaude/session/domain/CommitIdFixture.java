package dev.maxenceng.lazyclaude.session.domain;

public final class CommitIdFixture {

    private CommitIdFixture() {}

    public static CommitId commitId() {
        return new CommitId("a1b2c3d4e5f60718293a4b5c6d7e8f9012345678");
    }
}
