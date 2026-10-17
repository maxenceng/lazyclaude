package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Reads what a session's transcript shows, from where the last read ended. A transcript that cannot be read is an answer, never a failure. */
public final class TranscriptManager {

    private final TranscriptReadPort transcripts;

    public TranscriptManager(TranscriptReadPort transcripts) {
        this.transcripts = transcripts;
    }

    public TranscriptRead read(SessionId session, WorkingDirectory directory, TranscriptOffset from) {
        try {
            return transcripts.read(session, directory, from);
        } catch (TranscriptUnreadableException unreadable) {
            return new UnreadableTranscript(new Reason(unreadable.getMessage()));
        }
    }
}
