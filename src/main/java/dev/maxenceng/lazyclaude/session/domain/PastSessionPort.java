package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

/** Where the machine's past sessions are read from. */
public interface PastSessionPort {

    /**
     * Every past session already known, whatever project it belongs to, with where its {@code cwd} was located. It never
     * touches a transcript: what a transcript says arrives through {@link TranscriptWatchPort}.
     */
    List<LocatedPastSession> pastSessions();
}
