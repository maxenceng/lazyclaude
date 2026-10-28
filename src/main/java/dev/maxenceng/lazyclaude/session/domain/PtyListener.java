package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * Told what happens to a hosted session's pty, for a frontend that emulates it: that a session is about to be spawned, what it
 * wrote, and that it ended. {@link #output} comes on the host's reader thread, and always between the other two.
 */
public interface PtyListener {

    void launching(SessionId id, PaneSize size);

    void output(SessionId id, PtyBytes bytes);

    void ended(SessionId id);
}
