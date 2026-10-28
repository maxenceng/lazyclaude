package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.ticketClaude;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSizeOf;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostLaunch;
import dev.maxenceng.lazyclaude.session.domain.NewClaude;
import dev.maxenceng.lazyclaude.session.domain.ResumeClaude;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class PtyHostFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(20);

    private PtyHostFixture() {}

    public static PtyHostRepository hostRunning(String script) {
        return hostRunning(script, Map.of());
    }

    public static PtyHostRepository hostRunning(String script, Map<String, String> environment) {
        return new PtyHostRepository(List.of("/bin/sh", "-c", script, "sh"), environment);
    }

    public static PtyHostRepository hostOfCommand(String... command) {
        return new PtyHostRepository(List.of(command), Map.of());
    }

    public static HostLaunch newLaunchIn(SessionId id, Path directory, int columns, int rows) {
        return HostLaunch.builder()
                .command(new NewClaude(id))
                .directory(new WorkingDirectory(directory))
                .size(paneSizeOf(columns, rows))
                .build();
    }

    public static HostLaunch newLaunch(SessionId id) {
        return newLaunchIn(id, Path.of("/tmp"), 100, 30);
    }

    public static HostLaunch resumeLaunch(SessionId id) {
        return HostLaunch.builder()
                .command(new ResumeClaude(id))
                .directory(new WorkingDirectory(Path.of("/tmp")))
                .size(paneSizeOf(100, 30))
                .build();
    }

    public static HostLaunch ticketLaunch(SessionId id) {
        return HostLaunch.builder()
                .command(ticketClaude(id, Verb.REVIEW))
                .directory(new WorkingDirectory(Path.of("/tmp")))
                .size(paneSizeOf(100, 30))
                .build();
    }

    public static CompletableFuture<ExitCode> exitOf() {
        return new CompletableFuture<>();
    }

    public static ExitCode awaited(CompletableFuture<ExitCode> exit) throws Exception {
        return exit.get(PATIENCE.toSeconds(), TimeUnit.SECONDS);
    }

    public static String text(TerminalScreen screen) {
        return String.join(
                "\n",
                screen.rows().stream()
                        .map(ScreenRow::runs)
                        .map(runs -> String.join(
                                "", runs.stream().map(run -> run.text().value()).toList()))
                        .map(String::stripTrailing)
                        .toList());
    }

    public static TerminalScreen screenWhere(
            PtyHostRepository host, SessionId id, Predicate<TerminalScreen> expected) throws Exception {
        Instant until = Instant.now().plus(PATIENCE);
        AtomicBoolean timedOut = new AtomicBoolean();
        TerminalScreen screen = host.screenOf(id);
        while (!expected.test(screen) && !timedOut.get()) {
            Thread.sleep(POLL.toMillis());
            screen = host.screenOf(id);
            timedOut.set(Instant.now().isAfter(until));
        }
        if (timedOut.get()) {
            throw new TimeoutException("the screen never held what was expected; it showed:\n" + text(screen));
        }
        return screen;
    }

    public static TerminalScreen screenContaining(PtyHostRepository host, SessionId id, String fragment)
            throws Exception {
        return screenWhere(host, id, screen -> text(screen).contains(fragment));
    }
}
