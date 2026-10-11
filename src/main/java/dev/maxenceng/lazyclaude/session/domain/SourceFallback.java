package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

import dev.maxenceng.lazyclaude.session.published.Reason;

/** What a source of live sessions tells the domain when the registry is unusable and it asks {@code claude agents --json} instead. */
@FunctionalInterface
public interface SourceFallback {

    /** Said on each read that falls back, with why the registry could not be used and how often the sessions are asked for. */
    void askingClaudeAgents(Reason cause, Duration every);
}
