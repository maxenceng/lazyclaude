package dev.maxenceng.lazyclaude.session.domain;

/** Who owns a live session's process: lazyclaude, on a pty of its own, or something else. */
public enum Hosting {
    HOSTED,
    OUTSIDE;

    /** Whether a jump can bring the session's window forward: one lazyclaude hosts is in a dock tab, and never jumped to. */
    public boolean isJumpable() {
        return this == OUTSIDE;
    }
}
