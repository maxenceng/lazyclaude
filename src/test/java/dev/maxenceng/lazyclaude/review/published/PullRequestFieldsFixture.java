package dev.maxenceng.lazyclaude.review.published;

public final class PullRequestFieldsFixture {

    private PullRequestFieldsFixture() {}

    public static PullRequestNumber pullRequestNumber() {
        return new PullRequestNumber(23);
    }

    public static PullRequestNumber secondPullRequestNumber() {
        return new PullRequestNumber(24);
    }

    public static PullRequestTitle pullRequestTitle() {
        return new PullRequestTitle("PR and CI glyphs on the board");
    }

    public static PullRequestTitle secondPullRequestTitle() {
        return new PullRequestTitle("Status comes from the branch copy");
    }

    public static PullRequestTitle shortPullRequestTitle() {
        return new PullRequestTitle("Board glyphs");
    }
}
