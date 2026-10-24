package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.WatchService;

public final class WatchServiceStateFixture {

    private static final String LIMIT_REACHED = "User limit of inotify watches reached";

    private WatchServiceStateFixture() {}

    public static String limitReachedMessage() {
        return LIMIT_REACHED;
    }

    public static IOException limitReached() {
        return new IOException(LIMIT_REACHED);
    }

    public static Watching watching(WatchService service) {
        return new Watching(service);
    }

    public static NotWatching notWatchingForLimit() {
        return new NotWatching(LIMIT_REACHED);
    }
}
