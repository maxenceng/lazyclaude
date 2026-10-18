package dev.maxenceng.lazyclaude.review.published;

public final class CommitShaFixture {

    private CommitShaFixture() {}

    public static CommitSha commitSha() {
        return new CommitSha("9f3c2e1a0b8d7c6e5f4a3b2c1d0e9f8a7b6c5d4e");
    }

    public static CommitSha movedCommitSha() {
        return new CommitSha("4b1d0a7e6f5c4d3b2a1908f7e6d5c4b3a2918070");
    }
}
