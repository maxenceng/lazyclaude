package dev.maxenceng.lazyclaude.review.published;

public final class SummaryFixture {

    private SummaryFixture() {}

    public static Summary summary() {
        return new Summary("Looks close. Two nits.\nThe second is about naming.");
    }

    public static Summary noSummary() {
        return new Summary("");
    }
}
