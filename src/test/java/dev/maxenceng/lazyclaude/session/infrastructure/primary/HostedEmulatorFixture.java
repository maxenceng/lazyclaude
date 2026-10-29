package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.newHostedPayload;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

public final class HostedEmulatorFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration POLL = Duration.ofMillis(20);

    private HostedEmulatorFixture() {}

    public static SessionApplicationService serviceOver(HostPort hosts) {
        return sessionApplicationService(
                mock(LiveSessionPort.class),
                quietPastSessions(),
                emptyLaunchLogPort(),
                mock(SessionWatchPort.class),
                mock(TranscriptWatchPort.class),
                mock(WarningPort.class),
                hosts);
    }

    public static RunningSession startedOn(HostPort hosts, SessionApplicationService service) {
        when(hosts.spawn(any(), any(), any())).thenReturn(processId());
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        ArgumentCaptor<Consumer<PtyBytes>> output = ArgumentCaptor.captor();
        ArgumentCaptor<Consumer<ExitCode>> exit = ArgumentCaptor.captor();
        verify(hosts, atLeastOnce()).spawn(any(), output.capture(), exit.capture());
        return new RunningSession(id, output.getAllValues().getLast(), exit.getAllValues().getLast());
    }

    public static String textOf(EmulatorScreenView screen) {
        return String.join(
                        "\n",
                        screen.rows().stream()
                                .map(ScreenRow::runs)
                                .map(runs -> String.join("", runs.stream().map(run -> run.text().value()).toList()))
                                .map(String::stripTrailing)
                                .toList())
                .stripTrailing();
    }

    public static EmulatorScreenView screenContaining(HostedEmulatorQuery emulators, SessionId id, String fragment)
            throws Exception {
        Instant until = Instant.now().plus(PATIENCE);
        EmulatorScreenView screen = emulators.screenOf(id);
        while (!textOf(screen).contains(fragment) && Instant.now().isBefore(until)) {
            Thread.sleep(POLL.toMillis());
            screen = emulators.screenOf(id);
        }
        if (!textOf(screen).contains(fragment)) {
            throw new TimeoutException("the screen never held %s; it showed:%n%s".formatted(fragment, textOf(screen)));
        }
        return screen;
    }

    public static void emulatorsLeft(SessionId id, long count) throws Exception {
        String name = "emulator-%s".formatted(id.value());
        Instant until = Instant.now().plus(PATIENCE);
        while (alive(name) != count && Instant.now().isBefore(until)) {
            Thread.sleep(POLL.toMillis());
        }
        if (alive(name) != count) {
            throw new TimeoutException("%d emulator threads of %s ran, not %d".formatted(alive(name), id.value(), count));
        }
    }

    private static long alive(String name) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals(name))
                .count();
    }
}
