package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

/** Where the machine's live sessions are read from. */
public interface LiveSessionPort {

    /**
     * Every session whose process runs on this machine, whatever project it belongs to, liveness already
     * checked, with its status as Claude Code reported it.
     *
     * When the registry is unusable and another source answers, {@code fallback} is told, on every read that falls back.
     *
     * @throws SessionsUnavailableException when no source answers, with the reason
     */
    List<ReportedSession> liveSessions(SourceFallback fallback);
}
