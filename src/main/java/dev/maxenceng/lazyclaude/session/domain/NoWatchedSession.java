package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The user is looking at no session: the dock is not focused, the terminal is not, or no tab holds a live session. */
public record NoWatchedSession() implements Watched {

    @Override
    public boolean watches(SessionId session) {
        return false;
    }
}
