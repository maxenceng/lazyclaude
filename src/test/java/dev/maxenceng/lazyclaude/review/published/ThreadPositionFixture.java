package dev.maxenceng.lazyclaude.review.published;

public final class ThreadPositionFixture {

    private ThreadPositionFixture() {}

    public static PlacedThread placed(ThreadSide side, int first, int last) {
        return PlacedThread.builder()
                .side(side)
                .first(new DiffLineNumber(first))
                .last(new DiffLineNumber(last))
                .build();
    }

    public static PlacedThread rightOn(int line) {
        return placed(ThreadSide.RIGHT, line, line);
    }

    public static PlacedThread leftOn(int line) {
        return placed(ThreadSide.LEFT, line, line);
    }

    public static PlacedThread rightRange(int first, int last) {
        return placed(ThreadSide.RIGHT, first, last);
    }

    public static OutdatedThread outdated() {
        return new OutdatedThread();
    }
}
