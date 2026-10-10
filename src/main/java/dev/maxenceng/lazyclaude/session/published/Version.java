package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A Claude Code version as the registry writes it, such as {@code 2.1.296}. */
public record Version(String value) implements ClaudeVersion {

    public Version {
        Assert.notBlank("version", value);
    }
}
