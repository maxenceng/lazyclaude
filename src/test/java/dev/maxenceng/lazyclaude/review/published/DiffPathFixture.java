package dev.maxenceng.lazyclaude.review.published;

public final class DiffPathFixture {

    private DiffPathFixture() {}

    public static DiffPath diffPath() {
        return new DiffPath("src/Board.java");
    }

    public static DiffPath secondDiffPath() {
        return new DiffPath("src/Docs.java");
    }

    public static DiffPath oldDiffPath() {
        return new DiffPath("src/Old.java");
    }

    public static DiffPath newDiffPath() {
        return new DiffPath("src/New.java");
    }
}
