package dev.maxenceng.lazyclaude.session.published;

/** The session reports a status lazyclaude does not know, as a newer Claude Code may; it is listed all the same. */
public record Unknown() implements SessionState {}
