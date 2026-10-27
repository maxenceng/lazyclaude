package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The transcript could not be read, so the session has neither title nor opening prompt to show. */
public record UnreadableFacts(Reason reason) implements SessionFacts {

    public UnreadableFacts {
        Assert.notNull("reason", reason);
    }

    @Override
    public SessionTitle title() {
        return new NoTitle();
    }
}
