package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.Reason;

/** The transcripts could not be read, for the reason. */
public record UnreadableTranscript(Reason reason) implements TranscriptRead {

    public UnreadableTranscript {
        Assert.notNull("reason", reason);
    }
}
