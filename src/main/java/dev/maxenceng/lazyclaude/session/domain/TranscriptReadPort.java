package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Where the entries of a session's transcript are read from, live session or past. */
public interface TranscriptReadPort {

    /**
     * The complete entries written to the session's transcript from {@code from}, found by the session id and the directory it
     * runs in. A line still being written is left for the next read; an offset past the end of a shorter file reads from the start.
     * A session with no transcript yet reads none.
     *
     * @throws TranscriptUnreadableException when the transcripts cannot be read, with the reason
     */
    TranscriptRead read(SessionId session, WorkingDirectory directory, TranscriptOffset from);
}
