package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

/** What a source of live sessions tells the domain when the registry is unusable and it asks {@code claude agents --json} instead. */
@FunctionalInterface
public interface SourceFallback {

    /** Said once, naming why the registry could not be used and how often the sessions are asked for. */
    void askingClaudeAgents(String cause, Duration every);
}
