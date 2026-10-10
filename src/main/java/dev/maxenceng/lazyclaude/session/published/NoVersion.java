package dev.maxenceng.lazyclaude.session.published;

/** The source of the session does not report a version; {@code claude agents --json} does not. */
public record NoVersion() implements ClaudeVersion {}
