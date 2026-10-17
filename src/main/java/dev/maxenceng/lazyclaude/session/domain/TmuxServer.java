package dev.maxenceng.lazyclaude.session.domain;

/** The tmux server a pane belongs to, or nothing when the process was not told which. */
public sealed interface TmuxServer permits TmuxSocket, UnknownTmuxServer {

    /** Whether this server is known to be {@code other}, which an unknown one never is. */
    boolean isSameAs(TmuxServer other);
}
