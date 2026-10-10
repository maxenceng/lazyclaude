package dev.maxenceng.lazyclaude.session.published;

/** The Claude Code version a session runs, or nothing when its source does not say — never a stand-in. */
public sealed interface ClaudeVersion permits Version, NoVersion {}
