package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a prompt or a reply says, as the transcript wrote it. */
public record EntryText(String value) {

    public EntryText {
        Assert.notBlank("entry text", value);
    }
}
