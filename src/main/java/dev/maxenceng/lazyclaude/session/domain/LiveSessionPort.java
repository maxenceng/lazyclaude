package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

/** Where the machine's live sessions are read from. */
public interface LiveSessionPort {

    /**
     * Every session whose process runs on this machine, whatever project it belongs to, liveness already checked.
     *
     * @throws SessionsUnavailableException when no source answers, with the reason
     */
    List<LiveSession> liveSessions();
}
