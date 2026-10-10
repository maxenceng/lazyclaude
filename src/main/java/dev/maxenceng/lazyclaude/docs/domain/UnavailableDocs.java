package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A {@code docs/} that exists but cannot be listed; lazyclaude starts anyway and says why. */
public record UnavailableDocs(Reason reason) implements Docs {

    public UnavailableDocs {
        Assert.notNull("reason", reason);
    }
}
