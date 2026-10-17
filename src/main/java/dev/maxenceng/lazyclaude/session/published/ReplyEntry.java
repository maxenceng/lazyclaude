package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Something Claude said. */
public record ReplyEntry(TranscriptEntryText text) implements TranscriptEntry {

    public ReplyEntry {
        Assert.notNull("text", text);
    }
}
