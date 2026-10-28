package dev.maxenceng.lazyclaude.session.domain;

/**
 * The {@code status} Claude Code reports for a session, before lazyclaude's own states are drawn from it.
 * {@link #UNKNOWN} stands for a status this version of lazyclaude does not know.
 */
public enum ReportedStatus {
    BUSY,
    SHELL,
    IDLE,
    WAITING,
    UNKNOWN
}
