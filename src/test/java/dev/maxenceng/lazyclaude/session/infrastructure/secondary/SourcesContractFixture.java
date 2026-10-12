package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

public final class SourcesContractFixture {

    private static final Path HOME = Path.of(System.getProperty("user.home"));
    private static final Duration REGISTRATION_PATIENCE = Duration.ofSeconds(30);
    private static final Duration POLL = Duration.ofMillis(200);

    private SourcesContractFixture() {}

    public static List<String> fromTheRegistry() {
        return compared(new SessionRegistryRepository(
                HOME.resolve(".claude/sessions"), Path.of("/proc"), Path.of("/etc/machine-id"))
                .liveSessions());
    }

    public static List<String> fromClaudeAgents() {
        return compared(new ClaudeAgentsRepository(List.of("claude", "agents", "--json"), Duration.ofSeconds(30)).liveSessions());
    }

    public static PtyHostRepository hostOfTheRealClaude() {
        return new PtyHostRepository(List.of("claude"), System.getenv());
    }

    public static ProcessId spawnedInTheProjectRoot(PtyHostRepository host, HostedSessionId id) {
        return host.spawn(
                PtyHostFixture.newLaunchIn(id, Path.of(System.getProperty("user.dir")), 120, 40), code -> {});
    }

    public static List<Long> registeredPidsOnceListing(ProcessId pid) throws InterruptedException {
        Instant until = Instant.now().plus(REGISTRATION_PATIENCE);
        List<Long> pids = registeredPids();
        while (!pids.contains(pid.value()) && Instant.now().isBefore(until)) {
            Thread.sleep(POLL.toMillis());
            pids = registeredPids();
        }
        return pids;
    }

    private static List<Long> registeredPids() {
        return new SessionRegistryRepository(
                        HOME.resolve(".claude/sessions"), Path.of("/proc"), Path.of("/etc/machine-id"))
                .liveSessions().stream()
                .map(session -> session.pid().value())
                .toList();
    }

    private static List<String> compared(List<ReportedSession> sessions) {
        return sessions.stream()
                .map(session -> "pid=%d id=%s cwd=%s name=%s status=%s waitingFor=%s"
                        .formatted(
                                session.pid().value(),
                                session.id().value(),
                                session.directory().path(),
                                session.name().value(),
                                session.status(),
                                session.waitingFor()))
                .toList();
    }
}
