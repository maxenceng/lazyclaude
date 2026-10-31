package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Told which hosted session exited by itself and with what code, on the host's reader thread. */
@FunctionalInterface
public interface SessionExitListener {

    void exited(SessionId id, ExitCode code);
}
