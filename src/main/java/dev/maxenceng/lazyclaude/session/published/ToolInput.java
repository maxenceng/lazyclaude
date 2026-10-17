package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The text of a tool call's input that says what it was about, on one line. */
public record ToolInput(String value) implements ToolArgument {

    public ToolInput {
        Assert.notBlank("tool input", value);
    }
}
