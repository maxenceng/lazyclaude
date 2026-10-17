package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a session's row says of where it lives: {@code kitty} or {@code tmux}. */
public record ResidenceName(String value) {

    public ResidenceName {
        Assert.notBlank("residence name", value);
    }
}
