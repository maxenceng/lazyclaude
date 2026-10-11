package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The {@code /ticket} verb a session last ran on a ticket. */
public record LinkVerb(String value) {

    public LinkVerb {
        Assert.notBlank("verb", value);
    }
}
