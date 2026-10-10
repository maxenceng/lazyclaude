package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Watches every directory under {@code docs/} except {@code docs/backlog/}, because Java's watcher
 * does not reach into subdirectories on Linux: a directory that appears gets a watch of its own, and
 * an {@code OVERFLOW} registers the tree again. One save fires several events, so a change is
 * reported once the events have been quiet for the batch window. When a watch cannot be set up
 * ({@code fs.inotify.max_user_watches} used up, or no inotify) the tree is polled instead, with one
 * warning.
 */
public final class DocsWatchRepository implements DocsWatchPort, AutoCloseable {

    static final Duration BATCH_WINDOW = Duration.ofMillis(100);
    static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private static final Path DOCS = Path.of("docs");
    private static final String BACKLOG = "backlog";
    private static final String SEPARATOR = "/";
    private static final String WARNING =
            "lazyclaude: cannot watch %s (%s); checking it every %d s instead. If the cause is the watch limit, raise the fs.inotify.max_user_watches sysctl.";
    private static final String WORKER_NAME = "docs-watch";

    private final WatchServiceFactory services;
    private final DirectoryRegistrar registrar;
    private final PrintStream warnings;
    private final Duration batchWindow;
    private final Duration pollInterval;
    private final AtomicBoolean warned = new AtomicBoolean();
    private final List<WatchService> open = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();
    private final Map<Path, WatchKey> watched = new ConcurrentHashMap<>();

    public DocsWatchRepository(PrintStream warnings) {
        this(
                () -> FileSystems.getDefault().newWatchService(),
                (service, directory) -> directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
                warnings,
                BATCH_WINDOW,
                POLL_INTERVAL);
    }

    DocsWatchRepository(
            WatchServiceFactory services,
            DirectoryRegistrar registrar,
            PrintStream warnings,
            Duration batchWindow,
            Duration pollInterval) {
        this.services = services;
        this.registrar = registrar;
        this.warnings = warnings;
        this.batchWindow = batchWindow;
        this.pollInterval = pollInterval;
    }

    @Override
    public void watch(Project project, Runnable changed) {
        Path docs = project.root().resolve(DOCS);
        // deferred: a project without docs/ at start is never watched, so a docs/ created later shows up only after a restart — close it when someone starts a project that way.
        if (!Files.isDirectory(docs)) {
            return;
        }
        Path backlog = docs.resolve(BACKLOG);
        try {
            WatchService service = services.create();
            open.add(service);
            try {
                register(service, docs, backlog);
            } catch (IOException e) {
                closeQuietly(service);
                throw e;
            }
            start(() -> watchLoop(service, backlog, docs, changed));
        } catch (IOException e) {
            giveUpWatching(docs, e);
            Map<String, String> seen = snapshot(docs, Map.of());
            start(() -> pollLoop(docs, seen, changed));
        }
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        open.forEach(DocsWatchRepository::closeQuietly);
    }

    private void start(Runnable work) {
        workers.add(Thread.ofPlatform().daemon().name(WORKER_NAME).start(work));
    }

    private void watchLoop(WatchService service, Path backlog, Path docs, Runnable changed) {
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
        } catch (IOException e) {
            closeQuietly(service);
            giveUpWatching(docs, e);
            pollLoop(docs, snapshot(docs, Map.of()), changed);
        }
    }

    private boolean process(WatchService service, WatchKey key, Path backlog, Path docs) throws IOException {
        boolean relevant = false;
        Path directory = (Path) key.watchable();
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == OVERFLOW) {
                register(service, docs, backlog);
                relevant = true;
                continue;
            }
            Path entry = directory.resolve((Path) event.context());
            if (entry.equals(backlog)) {
                continue;
            }
            if (event.kind() == ENTRY_CREATE && Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                register(service, entry, backlog);
                relevant = true;
            } else if (event.kind() == ENTRY_DELETE && watched.containsKey(entry)) {
                forget(entry);
                relevant = true;
            } else if (listed(docs, entry)) {
                relevant = true;
            }
        }
        key.reset();
        return relevant;
    }

    private void register(WatchService service, Path tree, Path backlog) throws IOException {
        Files.walkFileTree(tree, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (directory.equals(backlog)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                try {
                    watched.put(directory, registrar.register(service, directory));
                } catch (NoSuchFileException vanished) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException failure) throws IOException {
                if (failure instanceof NoSuchFileException) {
                    return FileVisitResult.CONTINUE;
                }
                throw failure;
            }
        });
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
        Map<String, String> found = new HashMap<>();
        try (Stream<Path> files = Files.walk(docs)) {
            for (Path file : files.filter(path -> !path.equals(docs))
                    .filter(path -> listed(docs, path))
                    .toList()) {
                found.put(file.toString(), stamp(file));
            }
        } catch (IOException | UncheckedIOException unreadable) {
            return previous;
        }
        return found;
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

    private void giveUpWatching(Path docs, IOException cause) {
        if (warned.compareAndSet(false, true)) {
            warnings.println(WARNING.formatted(docs, cause.getMessage(), pollInterval.toSeconds()));
        }
    }

    private static void closeQuietly(WatchService service) {
        try {
            service.close();
        } catch (IOException ignored) {
            // a watch that will not close has nothing left to report
        }
    }
}
