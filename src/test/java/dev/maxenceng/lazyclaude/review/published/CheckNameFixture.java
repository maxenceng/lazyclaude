package dev.maxenceng.lazyclaude.review.published;

public final class CheckNameFixture {

    private CheckNameFixture() {}

    public static CheckName analyse() {
        return new CheckName("analyse");
    }

    public static CheckName build() {
        return new CheckName("build");
    }

    public static CheckName contract() {
        return new CheckName("contract");
    }

    public static CheckName lint() {
        return new CheckName("lint");
    }
}
