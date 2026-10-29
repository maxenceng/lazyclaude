package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** Text pasted into the pane, to reach the session as one paste. */
public record EmulatorPastePayload(String value) implements EmulatorInputPayload {

    public EmulatorPastePayload {
        Assert.notNull("pasted text", value);
    }
}
