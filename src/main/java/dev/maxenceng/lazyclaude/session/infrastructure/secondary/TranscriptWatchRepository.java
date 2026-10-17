package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.PollingFallback;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.TranscriptLocator;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * Keeps the transcripts current. Transcripts sit at a fixed depth, so it watches {@code ~/.claude/projects/} and each
 * folder in it itself, and a folder created later gets a watch of its own. A changed file is read once it has been quiet
 * for the settle time, not on every line it grows by; an {@code OVERFLOW} re-states every transcript. When a watch cannot
 * be set up the transcripts are re-stated on a timer instead, and the fallback is told why, naming the sysctl only this
 * adapter knows it depends on. The scan runs on this thread, never on the registry watcher's.
 */
public final class TranscriptWatchRepository implements TranscriptWatchPort, AutoCloseable {

    static final Duration SETTLE = Duration.ofSeconds(1);
    static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private static final Duration TICK = Duration.ofMillis(100);
    private static final String WORKER_NAME = "transcript-watch";
    private static final String CAUSE = "cannot watch %s (%s); if the watch limit is reached, raise fs.inotify.max_user_watches";

    private final Path transcriptsRoot;
    private final TranscriptRepository transcripts;
    private final WatchServiceFactory services;
    private final WatchKeyPoll keys;
    private final Duration settle;
    private final Duration pollInterval;
    private final List<WatchService> open = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();
    private final Semaphore rescans = new Semaphore(0);

    public TranscriptWatchRepository(Path transcriptsRoot, TranscriptRepository transcripts) {
        this(
                transcriptsRoot,
                transcripts,
                () -> FileSystems.getDefault().newWatchService(),
                WatchKeyPoll.platform(),
                SETTLE,
                POLL_INTERVAL);
    }

    TranscriptWatchRepository(
            Path transcriptsRoot,
            TranscriptRepository transcripts,
            WatchServiceFactory services,
            WatchKeyPoll keys,
            Duration settle,
            Duration pollInterval) {
        this.transcriptsRoot = transcriptsRoot;
        this.transcripts = transcripts;
        this.services = services;
        this.keys = keys;
        this.settle = settle;
        this.pollInterval = pollInterval;
    }

    @Override
    public void watch(
            TranscriptLocator locator, Runnable changed, PollingFallback fallback, CacheFallback cache, RootFallback root) {
        workers.add(Thread.ofPlatform()
                .daemon()
                .name(WORKER_NAME)
                .start(() -> run(locator, changed, fallback, cache, root)));
    }

    @Override
    public void rescanNow() {
        rescans.release();
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        open.forEach(TranscriptWatchRepository::closeQuietly);
    }

    // deferred: an exception other than a closed or interrupted watch ends this thread, and the past sessions stop updating with only a warning to say so — close it when a rescan can fail in a way the adapter does not turn into a value.
    private void run(
            TranscriptLocator locator, Runnable changed, PollingFallback fallback, CacheFallback cache, RootFallback root) {
        try {
            Optional<WatchService> service = watchOver(fallback);
            transcripts.rescanAll(locator, cache, root);
            changed.run();
            // deferred: a transcripts root that does not exist when lazyclaude starts is not watched, so past sessions of a Claude Code first run in this session appear at the next start — close it when that is seen.
            if (service.isPresent() || Files.isDirectory(transcriptsRoot)) {
                follow(service, locator, changed, fallback, cache, root);
            }
        } catch (InterruptedException | ClosedWatchServiceException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    private void follow(
            Optional<WatchService> initial,
            TranscriptLocator locator,
            Runnable changed,
            PollingFallback fallback,
            CacheFallback cache,
            RootFallback root)
            throws InterruptedException {
        Optional<WatchService> service = initial;
        Map<Path, Instant> pending = new HashMap<>();
        while (true) {
            boolean everything = false;
            if (service.isPresent()) {
                try {
                    everything = drain(service.get(), pending) | rescanRequested(Duration.ZERO);
                } catch (IOException cannotWatchFolder) {
                    closeQuietly(service.get());
                    giveUpWatching(cannotWatchFolder, fallback);
                    service = Optional.empty();
                }
            } else {
                rescanRequested(pollInterval);
                everything = true;
            }
            if (everything) {
                pending.clear();
                transcripts.rescanAll(locator, cache, root);
                changed.run();
            } else if (settleInto(pending, locator, cache)) {
                changed.run();
            }
        }
    }

    /** Whether a rescan was asked for, waiting up to {@code wait} for one; every request made so far is answered by the one rescan that follows. */
    private boolean rescanRequested(Duration wait) throws InterruptedException {
        boolean asked = rescans.tryAcquire(wait.toMillis(), TimeUnit.MILLISECONDS);
        return rescans.drainPermits() > 0 || asked;
    }

    private boolean settleInto(Map<Path, Instant> pending, TranscriptLocator locator, CacheFallback cache) {
        Instant quietSince = Instant.now().minus(settle);
        List<Path> settled = pending.entrySet().stream()
                .filter(change -> !change.getValue().isAfter(quietSince))
                .map(Map.Entry::getKey)
                .toList();
        if (settled.isEmpty()) {
            return false;
        }
        settled.forEach(pending::remove);
        transcripts.rescan(settled, locator, cache);
        return true;
    }

    private boolean drain(WatchService service, Map<Path, Instant> pending) throws InterruptedException, IOException {
        boolean everything = false;
        WatchKey key = keys.next(service, TICK);
        while (key != null) {
            everything |= process(key, service, pending);
            key = keys.next(service, Duration.ZERO);
        }
        if (everything) {
            watchFolders(service);
        }
        return everything;
    }

    // A loop, not a stream: registering throws IOException, which a lambda cannot throw and the build forbids wrapping.
    @SuppressWarnings("PMD.UseStreamsNotForLoops")
    private boolean process(WatchKey key, WatchService service, Map<Path, Instant> pending) throws IOException {
        boolean everything = false;
        Path directory = (Path) key.watchable();
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == OVERFLOW) {
                everything = true;
            } else if (event.context() instanceof Path name) {
                Path changed = directory.resolve(name);
                if (directory.equals(transcriptsRoot)) {
                    adoptFolder(changed, event.kind() == ENTRY_CREATE, service, pending);
                } else if (TranscriptFile.isTranscript(changed)) {
                    pending.put(changed, Instant.now());
                }
            }
        }
        key.reset();
        return everything;
    }

    // deferred: a folder removed between its create event and its registration gives up the whole watch for polling, with a warning that blames the watch limit — close it when that warning is seen without the limit reached.
    private void adoptFolder(Path folder, boolean created, WatchService service, Map<Path, Instant> pending)
            throws IOException {
        if (created && Files.isDirectory(folder)) {
            folder.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            transcriptsIn(folder).forEach(file -> pending.put(file, Instant.now()));
        }
    }

    private static List<Path> transcriptsIn(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(TranscriptFile::isTranscript).toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Optional<WatchService> watchOver(PollingFallback fallback) {
        if (!Files.isDirectory(transcriptsRoot)) {
            return Optional.empty();
        }
        WatchService service;
        try {
            service = services.create();
        } catch (IOException e) {
            giveUpWatching(e, fallback);
            return Optional.empty();
        }
        open.add(service);
        try {
            transcriptsRoot.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchFolders(service);
        } catch (IOException e) {
            closeQuietly(service);
            giveUpWatching(e, fallback);
            return Optional.empty();
        }
        return Optional.of(service);
    }

    /** Watches every folder; one already watched keeps its key, so this also catches a folder whose creation an overflow lost. */
    // A loop, not a stream: registering throws IOException, which a lambda cannot throw and the build forbids wrapping.
    @SuppressWarnings("PMD.UseStreamsNotForLoops")
    private void watchFolders(WatchService service) throws IOException {
        for (Path folder : foldersOf(transcriptsRoot)) {
            folder.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
    }

    private static List<Path> foldersOf(Path root) throws IOException {
        try (Stream<Path> entries = Files.list(root)) {
            return entries.filter(Files::isDirectory).toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private void giveUpWatching(IOException cause, PollingFallback fallback) {
        fallback.polling(new Reason(CAUSE.formatted(transcriptsRoot, FileFailure.causeOf(cause))), pollInterval);
    }

    private static void closeQuietly(WatchService service) {
        try {
            service.close();
        } catch (IOException ignored) {
            // a watch that will not close has nothing left to report
        }
    }
}
