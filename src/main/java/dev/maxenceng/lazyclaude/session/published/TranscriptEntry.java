package dev.maxenceng.lazyclaude.session.published;

/** One thing a transcript shows: a prompt, a reply, or a tool call. Tool results and thinking are not entries. */
public sealed interface TranscriptEntry permits PromptEntry, ReplyEntry, ToolCallEntry {}
