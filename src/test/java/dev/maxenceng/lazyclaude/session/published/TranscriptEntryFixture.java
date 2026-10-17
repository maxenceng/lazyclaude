package dev.maxenceng.lazyclaude.session.published;

public final class TranscriptEntryFixture {

    private TranscriptEntryFixture() {}

    public static TranscriptEntry prompt(String text) {
        return new PromptEntry(new TranscriptEntryText(text));
    }

    public static TranscriptEntry reply(String text) {
        return new ReplyEntry(new TranscriptEntryText(text));
    }

    public static TranscriptEntry toolCall(String tool, String argument) {
        return ToolCallEntry.builder().tool(new ToolName(tool)).argument(new ToolInput(argument)).build();
    }

    public static TranscriptEntry toolCallWithoutArgument(String tool) {
        return ToolCallEntry.builder().tool(new ToolName(tool)).argument(new NoToolInput()).build();
    }
}
