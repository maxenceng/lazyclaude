package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a prompt or a reply says, as the transcript wrote it. */
public record TranscriptEntryText(String value) {

    public TranscriptEntryText {
        Assert.notBlank("transcript entry text", value);
    }
}
