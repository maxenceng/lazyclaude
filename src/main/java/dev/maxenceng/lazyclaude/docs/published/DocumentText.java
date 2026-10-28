package dev.maxenceng.lazyclaude.docs.published;

/** What a readable document holds: markdown text, or nothing when the file is empty or only whitespace. */
public sealed interface DocumentText permits Content, EmptyDocument {}
