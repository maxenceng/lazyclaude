package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Something the user asked. */
public record PromptEntry(TranscriptEntryText text) implements TranscriptEntry {

    public PromptEntry {
        Assert.notNull("text", text);
    }
}
