package dev.maxenceng.lazyclaude.session.published;

/** What a tool call was about, such as the command {@code Bash} ran, or nothing when its input holds no text to show. */
public sealed interface ToolArgument permits ToolInput, NoToolInput {}
