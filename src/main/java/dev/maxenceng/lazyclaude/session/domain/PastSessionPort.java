package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.Map;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Where the machine's past sessions, and what any session's transcript says of the tickets it concerns, are read from. */
public interface PastSessionPort {

    /**
     * Every past session already known, whatever project it belongs to, with where its {@code cwd} was located. It never
     * touches a transcript: what a transcript says arrives through {@link TranscriptWatchPort}.
     */
    List<LocatedPastSession> pastSessions();

    /**
     * What every transcript known says that could link its session to a ticket, by session id: those of live sessions too,
     * since a live session is linked through its transcript. A session with no transcript yet is absent. It never touches a
     * transcript either.
     */
    Map<SessionId, LinkFacts> linkFacts();
}
