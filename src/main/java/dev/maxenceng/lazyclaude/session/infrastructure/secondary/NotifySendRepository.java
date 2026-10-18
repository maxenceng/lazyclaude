package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.DesktopNotification;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.NotificationId;

/**
 * Shows notifications through {@code notify-send -p}, which prints the id the desktop gave, and replaces one with {@code -r}.
 * {@code notify-send} cannot close one, so that goes over D-Bus through {@code gdbus}. A notification the user already
 * dismissed is closed all the same. The title and body follow {@code --}, so a reason that starts with a dash is not an option.
 */
public final class NotifySendRepository implements DesktopNotificationPort {

    // A reader blocks until the process exits, so each gets its own virtual thread rather than a slot of the shared pool.
    private static final Executor READERS = task -> Thread.ofVirtual().start(task);
    private static final String PRINT_ID = "-p";
    private static final String REPLACE = "-r";
    private static final String END_OF_OPTIONS = "--";
    private static final List<String> CLOSE_ARGUMENTS = List.of(
            "call",
            "--session",
            "--dest",
            "org.freedesktop.Notifications",
            "--object-path",
            "/org/freedesktop/Notifications",
            "--method",
            "org.freedesktop.Notifications.CloseNotification");
    private static final String ALREADY_GONE = "NotFound";
    private static final int SUCCESS = 0;
    private static final String TIMEOUT = "%s did not answer within %d s";
    private static final String EXITED = "%s exited with %d: %s";
    private static final String CANNOT_RUN = "cannot run %s: %s";
    private static final String INTERRUPTED = "interrupted while waiting for %s";
    private static final String UNREADABLE = "cannot read the answer of %s: %s";
    private static final String NO_ID = "%s printed no notification id";

    private final List<String> notifySend;
    private final List<String> gdbus;
    private final Duration patience;

    public NotifySendRepository(List<String> notifySend, List<String> gdbus, Duration patience) {
        this.notifySend = notifySend;
        this.gdbus = gdbus;
        this.patience = patience;
    }

    @Override
    public NotificationId show(DesktopNotification notification) {
        return idOf(run(notifySend, Stream.of(PRINT_ID), notification));
    }

    @Override
    public NotificationId replace(NotificationId id, DesktopNotification notification) {
        return idOf(run(notifySend, Stream.of(PRINT_ID, REPLACE, id.value()), notification));
    }

    @Override
    public void close(NotificationId id) {
        CommandOutput result = execute(Stream.concat(gdbus.stream(), Stream.concat(CLOSE_ARGUMENTS.stream(), Stream.of(id.value()))).toList(), gdbus);
        if (result.exit() != SUCCESS && !result.output().contains(ALREADY_GONE)) {
            throw new DesktopNotificationUnavailableException(
                    EXITED.formatted(gdbus.getFirst(), result.exit(), result.output().strip()));
        }
    }

    private String run(List<String> program, Stream<String> options, DesktopNotification notification) {
        CommandOutput result = execute(
                Stream.of(
                                program.stream(),
                                options,
                                Stream.of(END_OF_OPTIONS, notification.title().value(), notification.body().value()))
                        .flatMap(arguments -> arguments)
                        .toList(),
                program);
        if (result.exit() != SUCCESS) {
            throw new DesktopNotificationUnavailableException(
                    EXITED.formatted(program.getFirst(), result.exit(), result.output().strip()));
        }
        return result.output();
    }

    private NotificationId idOf(String output) {
        return output.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((first, last) -> last)
                .map(NotificationId::new)
                .orElseThrow(() -> new DesktopNotificationUnavailableException(NO_ID.formatted(notifySend.getFirst())));
    }

    private CommandOutput execute(List<String> command, List<String> program) {
        Process process = start(command, program);
        try {
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process, program), READERS);
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new DesktopNotificationUnavailableException(TIMEOUT.formatted(program.getFirst(), patience.toSeconds()));
            }
            return new CommandOutput(process.exitValue(), output.get(patience.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new DesktopNotificationUnavailableException(INTERRUPTED.formatted(program.getFirst()), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new DesktopNotificationUnavailableException(UNREADABLE.formatted(program.getFirst(), e.getMessage()), e);
        }
    }

    private static Process start(List<String> command, List<String> program) {
        try {
            return new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new DesktopNotificationUnavailableException(CANNOT_RUN.formatted(program.getFirst(), e.getMessage()), e);
        }
    }

    private static String readAll(Process process, List<String> program) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DesktopNotificationUnavailableException(UNREADABLE.formatted(program.getFirst(), e.getMessage()), e);
        }
    }
}
