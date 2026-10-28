package dev.maxenceng.lazyclaude.session.domain;

import java.util.Set;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Where what the transcripts of the sessions lazyclaude logged say is read from; no other transcript is met. */
public interface PastSessionPort {

    /**
     * What the transcripts of the {@code logged} sessions say, live ones included since a live session is linked through its
     * transcript, and which of them are gone. It never touches a transcript: what a transcript says arrives through
     * {@link TranscriptWatchPort}, and none is called gone before the first scan has listed them.
     */
    LoggedTranscripts transcriptsOf(Set<SessionId> logged);
}
