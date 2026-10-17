package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** What {@link SessionQuery} is asked a transcript with: the session, the directory it runs or ran in, and where the last read of it ended. */
public record TranscriptPayload(SessionId session, Path directory, TranscriptOffset offset) {

    public TranscriptPayload {
        Assert.notNull("session", session);
        Assert.notNull("offset", offset);
    }

    WorkingDirectory workingDirectory() {
        return new WorkingDirectory(directory);
    }
}
