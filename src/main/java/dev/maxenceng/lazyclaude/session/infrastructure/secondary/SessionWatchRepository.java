package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;

/**
 * Tells the domain when the session registry may have changed: at once when Claude Code rewrites a
 * {@code .json} record (a {@code .key} file is not one), and on a sweep every few seconds besides, because
 * a session whose process died never rewrites its record. One thread carries both, so a change is never
 * reported twice at once, and a batch of events never outlasts a sweep, so a registry rewritten without
 * pause still reports. When the registry cannot be put under a watch the sweep carries on alone, with
 * one warning.
 */
public final class SessionWatchRepository implements SessionWatchPort, AutoCloseable {

    static final Duration BATCH_WINDOW = Duration.ofMillis(100);
    public static final Duration SWEEP_INTERVAL = Duration.ofSeconds(2);

    private static final String RECORD_SUFFIX = ".json";
    private static final String WARNING = "lazyclaude: cannot watch %s (%s); checking sessions every %d s instead.";
    private static final String WORKER_NAME = "session-watch";

    private final Path sessions;
    private final WatchServiceFactory services;
    private final PrintStream warnings;
    private final Duration batchWindow;
    private final Duration sweepInterval;
    private final List<WatchService> open = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();

    public SessionWatchRepository(Path sessions, PrintStream warnings) {
        this(sessions, () -> FileSystems.getDefault().newWatchService(), warnings, BATCH_WINDOW, SWEEP_INTERVAL);
    }

    SessionWatchRepository(
            Path sessions,
            WatchServiceFactory services,
            PrintStream warnings,
            Duration batchWindow,
            Duration sweepInterval) {
        this.sessions = sessions;
        this.services = services;
        this.warnings = warnings;
        this.batchWindow = batchWindow;
        this.sweepInterval = sweepInterval;
    }

    @Override
    public void watch(Runnable changed) {
        Optional<WatchService> service = watchOver();
        workers.add(Thread.ofPlatform().daemon().name(WORKER_NAME).start(() -> loop(service, changed)));
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        open.forEach(SessionWatchRepository::closeQuietly);
    }

    private Optional<WatchService> watchOver() {
        WatchService service;
        try {
            service = services.create();
        } catch (IOException e) {
            giveUpWatching(e);
            return Optional.empty();
        }
        open.add(service);
        try {
            sessions.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            closeQuietly(service);
            giveUpWatching(e);
            return Optional.empty();
        }
        return Optional.of(service);
    }

    // deferred: an exception other than a closed or interrupted watch ends this thread without a word — close it when a re-read can fail in a way the domain does not turn into a value.
    private void loop(Optional<WatchService> service, Runnable changed) {
        try {
            while (true) {
                boolean relevant = service.isPresent() ? waitForEvents(service.get()) : sleepThroughSweep();
                if (relevant) {
                    changed.run();
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean sleepThroughSweep() throws InterruptedException {
        Thread.sleep(sweepInterval.toMillis());
        return true;
    }

    private boolean waitForEvents(WatchService service) throws InterruptedException {
        WatchKey first = service.poll(sweepInterval.toMillis(), TimeUnit.MILLISECONDS);
        if (first == null) {
            return true;
        }
        boolean relevant = process(first);
        long deadline = System.nanoTime() + sweepInterval.toNanos();
        WatchKey more = service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS);
        while (more != null && System.nanoTime() - deadline < 0) {
            relevant |= process(more);
            more = service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (more != null) {
            relevant |= process(more);
        }
        return relevant;
    }

    private static boolean process(WatchKey key) {
        boolean relevant = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            relevant |= event.kind() == OVERFLOW || isRecord(event);
        }
        key.reset();
        return relevant;
    }

    private static boolean isRecord(WatchEvent<?> event) {
        return event.context() instanceof Path name && name.toString().endsWith(RECORD_SUFFIX);
    }

    private void giveUpWatching(IOException cause) {
        warnings.println(WARNING.formatted(sessions, cause.getMessage(), sweepInterval.toSeconds()));
    }

    private static void closeQuietly(WatchService service) {
        try {
            service.close();
        } catch (IOException ignored) {
            // a watch that will not close has nothing left to report
        }
    }
}
