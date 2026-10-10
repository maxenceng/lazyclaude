package dev.maxenceng.lazyclaude.session.published;

/** The {@code status} Claude Code reports for a session, before lazyclaude's own states are drawn from it. */
public enum ReportedStatus {
    BUSY,
    SHELL,
    IDLE,
    WAITING
}
