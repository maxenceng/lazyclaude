package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;

public final class RegistryContractFixture {

    private static final Path REGISTRY = Path.of(System.getProperty("user.home")).resolve(".claude/sessions");
    private static final Duration REGISTRATION_PATIENCE = Duration.ofSeconds(30);
    private static final String ENTER = "\r";
    private static final Duration POLL = Duration.ofMillis(200);

    private RegistryContractFixture() {}

    public static PtyHostRepository hostOfTheRealClaude() {
        return new PtyHostRepository(List.of("claude"), System.getenv());
    }

    public static ProcessId spawnedInTheProjectRoot(PtyHostRepository host, SessionId id) {
        return host.spawn(
                PtyHostFixture.newLaunchIn(id, Path.of(System.getProperty("user.dir")), 120, 40), bytes -> {}, code -> {});
    }

    public static Optional<ReportedSession> recordOnceWritten(ProcessId pid) throws InterruptedException {
        return recordWhere(pid, record -> true);
    }

    public static Optional<ReportedSession> recordOnceIdle(ProcessId pid) throws InterruptedException {
        return recordWhere(pid, record -> record.status() == ReportedStatus.IDLE);
    }

    public static Optional<ReportedSession> recordOnceNamingAnotherSessionThan(ProcessId pid, SessionId launched)
            throws InterruptedException {
        return recordWhere(pid, record -> !record.session().equals(launched));
    }

    public static void typedThenEntered(PtyHostRepository host, SessionId id, String command) {
        host.write(id, new PtyBytes((command + ENTER).getBytes(StandardCharsets.UTF_8)));
    }

    private static Optional<ReportedSession> recordWhere(ProcessId pid, Predicate<ReportedSession> expected)
            throws InterruptedException {
        SessionRegistryRepository registry = new SessionRegistryRepository(REGISTRY);
        Instant until = Instant.now().plus(REGISTRATION_PATIENCE);
        Optional<ReportedSession> record = registry.recordOf(pid);
        while (record.filter(expected).isEmpty() && Instant.now().isBefore(until)) {
            Thread.sleep(POLL.toMillis());
            record = registry.recordOf(pid);
        }
        return record;
    }

    public static String recordText(ProcessId pid) throws java.io.IOException {
        return Files.readString(REGISTRY.resolve(pid.value() + ".json"));
    }
}
