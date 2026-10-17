package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Whether the user is looking at a session right now, and which: the one whose dock tab is selected while the dock and the terminal both have focus. */
public sealed interface Watched permits WatchedSession, NoWatchedSession {

    boolean watches(SessionId session);
}
