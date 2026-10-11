package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The title Claude Code wrote for a session; the last one in its transcript. */
public record AiTitle(String value) implements SessionTitle {

    public AiTitle {
        Assert.notBlank("ai title", value);
    }
}
