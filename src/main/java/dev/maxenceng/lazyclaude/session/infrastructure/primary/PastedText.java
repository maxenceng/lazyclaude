package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Text pasted into the pane, to reach the session as one paste. */
public record PastedText(String value) implements TerminalInput {

    public PastedText {
        Assert.notNull("pasted text", value);
    }
}
