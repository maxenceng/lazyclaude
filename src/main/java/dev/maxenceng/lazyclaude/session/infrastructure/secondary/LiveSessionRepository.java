package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;

/**
 * The machine's live sessions from the registry, or from {@code claude agents --json} while the registry
 * is unusable. The registry is tried first on every read, so it is used again as soon as it works; the
 * fallback is warned about once, naming why.
 */
public final class LiveSessionRepository implements LiveSessionPort {

    private static final String WARNING = "lazyclaude: %s; asking claude agents --json every %d s instead.";

    private final SessionRegistryRepository registry;
    private final ClaudeAgentsRepository agents;
    private final PrintStream warnings;
    private final Duration poll;
    private final AtomicBoolean warned = new AtomicBoolean();

    public LiveSessionRepository(
            SessionRegistryRepository registry, ClaudeAgentsRepository agents, PrintStream warnings, Duration poll) {
        this.registry = registry;
        this.agents = agents;
        this.warnings = warnings;
        this.poll = poll;
    }

    @Override
    public List<LiveSession> liveSessions() {
        SessionsUnavailableException unusable;
        try {
            return registry.liveSessions();
        } catch (SessionsUnavailableException registryFailure) {
            unusable = registryFailure;
        }
        try {
            List<LiveSession> live = agents.liveSessions();
            warnOnce(unusable);
            return live;
        } catch (SessionsUnavailableException agentsFailure) {
            throw new SessionsUnavailableException(
                    "%s; %s".formatted(unusable.getMessage(), agentsFailure.getMessage()), agentsFailure);
        }
    }

    private void warnOnce(SessionsUnavailableException registryFailure) {
        if (warned.compareAndSet(false, true)) {
            warnings.println(WARNING.formatted(registryFailure.getMessage(), poll.toSeconds()));
        }
    }
}
