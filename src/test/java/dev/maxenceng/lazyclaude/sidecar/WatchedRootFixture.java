package dev.maxenceng.lazyclaude.sidecar;

public final class WatchedRootFixture {

    private WatchedRootFixture() {}

    public static WatchedRoot watchedRoot() {
        return new WatchedRoot("docs/watch");
    }

    public static Runnable failingToOpen() {
        return () -> {
            throw new IllegalStateException("cannot watch the project");
        };
    }
}
