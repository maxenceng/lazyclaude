package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The reason Claude Code gives for a waiting session, such as {@code permission prompt}. */
public record WaitingReason(String value) implements WaitingFor {

    private static final String PERMISSION_PROMPT = "permission prompt";

    public WaitingReason {
        Assert.notBlank("waiting reason", value);
    }

    public boolean isPermissionPrompt() {
        return value.equals(PERMISSION_PROMPT);
    }
}
