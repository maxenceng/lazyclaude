package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.SourceFallback;

/**
 * The machine's live sessions from the registry, or from {@code claude agents --json} while the registry
 * is unusable. The registry is tried first on every read, so it is used again as soon as it works; the
 * fallback is warned about once, naming why.
 */
public final class LiveSessionRepository implements LiveSessionPort {

    private final SessionRegistryRepository registry;
    private final ClaudeAgentsRepository agents;
    private final Duration sweep;
    private final AtomicBoolean warned = new AtomicBoolean();

    public LiveSessionRepository(
            SessionRegistryRepository registry, ClaudeAgentsRepository agents, Duration sweep) {
        this.registry = registry;
        this.agents = agents;
        this.sweep = sweep;
    }

    @Override
    public List<ReportedSession> liveSessions(SourceFallback fallback) {
        SessionsUnavailableException unusable;
        try {
            return registry.liveSessions();
        } catch (SessionsUnavailableException registryFailure) {
            unusable = registryFailure;
        }
        try {
            List<ReportedSession> live = agents.liveSessions();
            warnOnce(unusable, fallback);
            return live;
        } catch (SessionsUnavailableException agentsFailure) {
            throw new SessionsUnavailableException(
                    "%s; %s".formatted(unusable.getMessage(), agentsFailure.getMessage()), agentsFailure);
        }
    }

    private void warnOnce(SessionsUnavailableException registryFailure, SourceFallback fallback) {
        if (warned.compareAndSet(false, true)) {
            fallback.askingClaudeAgents(registryFailure.getMessage(), sweep);
        }
    }
}
