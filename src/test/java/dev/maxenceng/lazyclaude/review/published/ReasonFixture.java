package dev.maxenceng.lazyclaude.review.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("gh: not logged in");
    }

    public static Reason offline() {
        return new Reason("gh pr list exited with 1: error connecting to api.github.com");
    }
}
