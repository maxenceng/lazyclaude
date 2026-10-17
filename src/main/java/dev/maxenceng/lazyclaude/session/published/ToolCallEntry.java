package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A tool Claude called, with what it was about. */
public record ToolCallEntry(ToolName tool, ToolArgument argument) implements TranscriptEntry {

    public ToolCallEntry {
        Assert.notNull("tool", tool);
        Assert.notNull("argument", argument);
    }

    public static ToolCallEntryBuilder builder() {
        return new ToolCallEntryBuilder();
    }

    public static final class ToolCallEntryBuilder {
        private ToolName tool;
        private ToolArgument argument;

        private ToolCallEntryBuilder() {}

        public ToolCallEntryBuilder tool(ToolName tool) {
            this.tool = tool;
            return this;
        }

        public ToolCallEntryBuilder argument(ToolArgument argument) {
            this.argument = argument;
            return this;
        }

        public ToolCallEntry build() {
            return new ToolCallEntry(tool, argument);
        }
    }
}
