package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.LiveSession;

public final class SourcesContractFixture {

    private static final Path HOME = Path.of(System.getProperty("user.home"));

    private SourcesContractFixture() {}

    public static List<String> fromTheRegistry() {
        return compared(new SessionRegistryRepository(
                HOME.resolve(".claude/sessions"), Path.of("/proc"), Path.of("/etc/machine-id"))
                .liveSessions());
    }

    public static List<String> fromClaudeAgents() {
        return compared(new ClaudeAgentsRepository(List.of("claude", "agents", "--json"), Duration.ofSeconds(30)).liveSessions());
    }

    private static List<String> compared(List<LiveSession> sessions) {
        return sessions.stream()
                .map(session -> "pid=%d id=%s cwd=%s name=%s state=%s"
                        .formatted(
                                session.pid().value(),
                                session.id().value(),
                                session.directory().value(),
                                session.name().value(),
                                session.state()))
                .toList();
    }
}
