package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The transcript could not be read now, so what is known of the session is what an earlier read left, if any. */
public record UnreadableTranscript(Reason reason) implements TranscriptReading {

    public UnreadableTranscript {
        Assert.notNull("reason", reason);
    }
}
