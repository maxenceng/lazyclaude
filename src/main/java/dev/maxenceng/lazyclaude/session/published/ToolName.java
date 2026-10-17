package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The tool Claude called, such as {@code Bash}. */
public record ToolName(String value) {

    public ToolName {
        Assert.notBlank("tool name", value);
    }
}
