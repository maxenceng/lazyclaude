package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

public final class TranscriptWatchFixture {

    private static final Duration SHORT_SETTLE = Duration.ofMillis(400);
    private static final Duration SHORT_POLL = Duration.ofMillis(300);

    private TranscriptWatchFixture() {}

    static TranscriptRepository repositoryOver(Path projects, Path cache) {
        return new TranscriptRepository(projects, new TranscriptCacheFile(cache));
    }

    static TranscriptWatchRepository watcherOver(Path projects, TranscriptRepository transcripts) {
        return new TranscriptWatchRepository(
                projects,
                transcripts,
                () -> FileSystems.getDefault().newWatchService(),
                WatchKeyPoll.platform(),
                SHORT_SETTLE,
                SHORT_POLL);
    }

    static TranscriptWatchRepository watcherLosingEvents(
            Path projects, TranscriptRepository transcripts, LostEventsPoll keys) {
        return new TranscriptWatchRepository(
                projects,
                transcripts,
                () -> FileSystems.getDefault().newWatchService(),
                keys,
                SHORT_SETTLE,
                SHORT_POLL);
    }

    static TranscriptWatchRepository watcherWithoutInotify(Path projects, TranscriptRepository transcripts) {
        return new TranscriptWatchRepository(
                projects,
                transcripts,
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
                WatchKeyPoll.platform(),
                SHORT_SETTLE,
                SHORT_POLL);
    }

    public static Duration settleTime() {
        return SHORT_SETTLE;
    }

    public static Duration pollTime() {
        return SHORT_POLL;
    }

    public static Duration quietLongerThanTheSettleTime() {
        return SHORT_SETTLE.multipliedBy(3);
    }
}
