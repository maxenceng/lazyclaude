package dev.maxenceng.lazyclaude.review.published;

public final class HeadCommitFixture {

    private HeadCommitFixture() {}

    public static HeadCommit headCommit() {
        return new HeadCommit("0a1b2c3d4e5f60718293a4b5c6d7e8f901234567");
    }

    public static HeadCommit secondHeadCommit() {
        return new HeadCommit("7654321098fedcba0987654321fedcba09876543");
    }
}
