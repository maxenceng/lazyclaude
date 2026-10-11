package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.domain.WatchFallback;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Watches every directory under {@code docs/} except {@code docs/backlog/}, because Java's watcher
 * does not reach into subdirectories on Linux: a directory that appears gets a watch of its own, and
 * an {@code OVERFLOW} registers the tree again. One save fires several events, so a change is
 * reported once the events have been quiet for the batch window. When a watch cannot be set up
 * ({@code fs.inotify.max_user_watches} used up, or no inotify) the tree is polled instead, and the
 * fallback is told why, naming the sysctl only this adapter knows it depends on.
 */
public final class DocsWatchRepository implements DocsWatchPort, AutoCloseable {

    static final Duration BATCH_WINDOW = Duration.ofMillis(100);
    static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private static final Path DOCS = Path.of("docs");
    private static final String BACKLOG = "backlog";
    private static final String SEPARATOR = "/";
    private static final String WORKER_NAME = "docs-watch";
    private static final String WATCH_LIMIT_ADVICE = "%s; if the watch limit is reached, raise fs.inotify.max_user_watches";

    private final WatchServiceFactory services;
    private final DirectoryRegistrar registrar;
    private final Duration batchWindow;
    private final Duration pollInterval;
    private final List<WatchService> open = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();
    private final Map<Path, WatchKey> watched = new ConcurrentHashMap<>();

    public DocsWatchRepository() {
        this(
                () -> FileSystems.getDefault().newWatchService(),
                (service, directory) -> directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
                BATCH_WINDOW,
                POLL_INTERVAL);
    }

    DocsWatchRepository(
            WatchServiceFactory services,
            DirectoryRegistrar registrar,
            Duration batchWindow,
            Duration pollInterval) {
        this.services = services;
        this.registrar = registrar;
        this.batchWindow = batchWindow;
        this.pollInterval = pollInterval;
    }

    @Override
    public void watch(Project project, Runnable changed, WatchFallback fallback) {
        Path docs = project.root().resolve(DOCS);
        // deferred: a project without docs/ at start is never watched, so a docs/ created later shows up only after a restart — close it when someone starts a project that way.
        if (!Files.isDirectory(docs)) {
            return;
        }
        Path backlog = docs.resolve(BACKLOG);
        try {
            WatchService service = watchOver(docs, backlog);
            start(() -> watchLoop(service, backlog, docs, changed, fallback));
        } catch (WatchUnavailableException unavailable) {
            giveUpWatching(unavailable, fallback);
            Map<String, String> seen = snapshot(docs, Map.of());
            start(() -> pollLoop(docs, seen, changed));
        }
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        open.forEach(DocsWatchRepository::closeQuietly);
    }

    /** A watch service with the whole tree registered on it; one that fails partway is closed, not left holding watches. */
    private WatchService watchOver(Path docs, Path backlog) {
        WatchService service;
        try {
            service = services.create();
        } catch (IOException e) {
            throw new WatchUnavailableException(docs, e);
        }
        open.add(service);
        try {
            register(service, docs, backlog, docs);
        } catch (WatchUnavailableException e) {
            closeQuietly(service);
            throw e;
        }
        return service;
    }

    private void start(Runnable work) {
        workers.add(Thread.ofPlatform().daemon().name(WORKER_NAME).start(work));
    }

    // deferred: docs/ deleted and created again while running is not watched again until a restart — close it when a checkout that swaps docs/ wholesale is used day to day.
    // deferred: an exception other than a closed or interrupted watch ends this thread, and the docs stop updating with only a warning to say so — close it when a re-read can fail in a way the domain does not turn into a value.
    private void watchLoop(WatchService service, Path backlog, Path docs, Runnable changed, WatchFallback fallback) {
        try {
            while (true) {
                boolean relevant = process(service, service.take(), backlog, docs);
                WatchKey more = service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS);
                while (more != null) {
                    relevant |= process(service, more, backlog, docs);
                    more = service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS);
                }
                if (relevant) {
                    changed.run();
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException stopped) {
            Thread.currentThread().interrupt();
        } catch (WatchUnavailableException unavailable) {
            closeQuietly(service);
            giveUpWatching(unavailable, fallback);
            pollLoop(docs, snapshot(docs, Map.of()), changed);
        }
    }

    private boolean process(WatchService service, WatchKey key, Path backlog, Path docs) {
        Path directory = (Path) key.watchable();
        boolean relevant = key.pollEvents().stream()
                .map(event -> handle(service, directory, event, backlog, docs))
                .reduce(false, Boolean::logicalOr);
        key.reset();
        return relevant;
    }

    /** Whether {@code event} changes what the Docs panel lists; every event is handled, so a relevant one does not stop the rest. */
    private boolean handle(WatchService service, Path directory, WatchEvent<?> event, Path backlog, Path docs) {
        if (event.kind() == OVERFLOW) {
            register(service, docs, backlog, docs);
            return true;
        }
        Path entry = directory.resolve((Path) event.context());
        if (entry.equals(backlog)) {
            return false;
        }
        if (event.kind() == ENTRY_CREATE && Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
            register(service, entry, backlog, docs);
            return true;
        }
        if (event.kind() == ENTRY_DELETE && watched.containsKey(entry)) {
            forget(entry);
            return true;
        }
        return listed(docs, entry);
    }

    private void register(WatchService service, Path tree, Path backlog, Path docs) {
        try {
            Files.walkFileTree(tree, new TreeRegistration(service, registrar, backlog, watched));
        } catch (IOException e) {
            throw new WatchUnavailableException(docs, e);
        }
    }

    /**
     * Cancels the watches on a folder that left its path and on everything beneath it: Linux keeps a
     * moved folder's watch, and a later register of its new path would hand back that key, still
     * naming the old one.
     */
    private void forget(Path folder) {
        watched.entrySet().removeIf(watch -> {
            boolean gone = watch.getKey().startsWith(folder);
            if (gone) {
                watch.getValue().cancel();
            }
            return gone;
        });
    }

    private void pollLoop(Path docs, Map<String, String> first, Runnable changed) {
        Map<String, String> seen = first;
        try {
            while (true) {
                Thread.sleep(pollInterval.toMillis());
                Map<String, String> now = snapshot(docs, seen);
                if (!now.equals(seen)) {
                    seen = now;
                    changed.run();
                }
            }
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    private static Map<String, String> snapshot(Path docs, Map<String, String> previous) {
        try (Stream<Path> files = Files.walk(docs)) {
            return files.filter(path -> !path.equals(docs))
                    .filter(path -> listed(docs, path))
                    .collect(Collectors.toMap(Path::toString, DocsWatchRepository::stamp));
        } catch (IOException | UncheckedIOException unreadable) {
            return previous;
        }
    }

    private static boolean listed(Path docs, Path file) {
        List<String> segments = new ArrayList<>();
        docs.relativize(file).forEach(segment -> segments.add(segment.toString()));
        return new DocumentPath(String.join(SEPARATOR, segments)).listed();
    }

    private static String stamp(Path file) {
        try {
            return "%s/%d".formatted(Files.getLastModifiedTime(file), Files.size(file));
        } catch (IOException vanished) {
            return "";
        }
    }

    private void giveUpWatching(WatchUnavailableException unavailable, WatchFallback fallback) {
        fallback.polling(new Reason(WATCH_LIMIT_ADVICE.formatted(unavailable.getMessage())), pollInterval);
    }

    private static void closeQuietly(WatchService service) {
        try {
            service.close();
        } catch (IOException ignored) {
            // a watch that will not close has nothing left to report
        }
    }
}
