package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.SourceFallback;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * The machine's live sessions from the registry, or from {@code claude agents --json} while the registry
 * is unusable. The registry is tried first on every read, so it is used again as soon as it works; each
 * read that falls back tells the fallback why.
 */
// deferred: the choice of source is made here rather than in the domain, so the port carries a SourceFallback the two leaf sources ignore — close it when the domain has to know which source answered.
public final class LiveSessionRepository implements LiveSessionPort {

    private final SessionRegistryRepository registry;
    private final ClaudeAgentsRepository agents;
    private final Duration sweep;

    public LiveSessionRepository(SessionRegistryRepository registry, ClaudeAgentsRepository agents) {
        this(registry, agents, SessionWatchRepository.SWEEP_INTERVAL);
    }

    LiveSessionRepository(SessionRegistryRepository registry, ClaudeAgentsRepository agents, Duration sweep) {
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
            fallback.askingClaudeAgents(new Reason(unusable.getMessage()), sweep);
            return live;
        } catch (SessionsUnavailableException agentsFailure) {
            throw new SessionsUnavailableException(
                    "%s; %s".formatted(unusable.getMessage(), agentsFailure.getMessage()), agentsFailure);
        }
    }
}
