package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Watches trees of directories, because Java's watcher does not reach into subdirectories on Linux: every directory a scope
 * descends into gets a watch of its own, one that appears later gets one when it does, and an {@code OVERFLOW} registers the
 * scope again. One save fires several events, so a change is reported once the events have been quiet for the batch window, or
 * once the maximum wait has passed under a stream that never goes quiet. It reports that something changed, never what.
 * A tree that cannot be put under a watch ({@code fs.inotify.max_user_watches} used up, or no inotify) is checked on a timer
 * instead, and the owner is told once for that tree, however often it is dropped and added again.
 */
public final class TreeWatcher implements AutoCloseable {

    static final Duration BATCH_WINDOW = Duration.ofMillis(100);
    static final Duration MAX_WAIT = Duration.ofSeconds(1);
    static final Duration POLL_INTERVAL = Duration.ofSeconds(5);

    private static final String NOT_STARTED = "the watcher was not started";
    private static final String WORKER_NAME = "tree-watch";
    private static final String CAUSE = "cannot watch %s (%s; if the watch limit is reached, raise fs.inotify.max_user_watches)";

    private final WatchServiceFactory services;
    private final DirectoryRegistrar registrar;
    private final Duration batchWindow;
    private final Duration maxWait;
    private final Duration pollInterval;
    private final Map<Path, TreeScope> scopes = new ConcurrentHashMap<>();
    private final Map<Path, WatchKey> watched = new ConcurrentHashMap<>();
    private final Map<Path, Map<String, String>> polled = new ConcurrentHashMap<>();
    private final Set<Path> warnedOf = ConcurrentHashMap.newKeySet();
    private volatile WatchServiceState state = new NotWatching(NOT_STARTED);
    private volatile TreePolling polling = (_, _, _) -> {};
    private volatile Thread worker;

    public TreeWatcher() {
        this(
                () -> FileSystems.getDefault().newWatchService(),
                (service, directory) -> directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
                BATCH_WINDOW,
                MAX_WAIT,
                POLL_INTERVAL);
    }

    TreeWatcher(
            WatchServiceFactory services,
            DirectoryRegistrar registrar,
            Duration batchWindow,
            Duration maxWait,
            Duration pollInterval) {
        this.services = services;
        this.registrar = registrar;
        this.batchWindow = batchWindow;
        this.maxWait = maxWait;
        this.pollInterval = pollInterval;
    }

    /**
     * Runs {@code changed} after each batch of changes, on a thread of the watcher's own, for as long as the process lives. A
     * machine that cannot watch at all leaves every tree polled, and {@code unwatchable} is told for each as it is added.
     */
    public void start(Runnable changed, TreePolling unwatchable) {
        this.polling = unwatchable;
        state = WatchServiceState.of(services);
        worker = Thread.ofPlatform().daemon().name(WORKER_NAME).start(() -> run(changed));
    }

    /** Puts {@code scope} under watch, or under polling when it cannot be watched; a root that is not a directory is not watched. */
    public synchronized void add(TreeScope scope) {
        if (!Files.isDirectory(scope.root()) || scopes.containsKey(scope.root())) {
            return;
        }
        scopes.put(scope.root(), scope);
        register(scope, scope.root());
    }

    /** Stops watching the tree that starts at {@code root}, and polling it. */
    public synchronized void drop(Path root) {
        scopes.remove(root);
        polled.remove(root);
        forget(root);
    }

    /** Whether the tree that starts at {@code root} is being watched or polled. */
    public boolean covers(Path root) {
        return scopes.containsKey(root);
    }

    @Override
    public void close() {
        Optional.ofNullable(worker).ifPresent(Thread::interrupt);
        if (state instanceof Watching(WatchService service)) {
            closeQuietly(service);
        }
    }

    private void register(TreeScope scope, Path from) {
        switch (state) {
            case Watching(WatchService service) -> registerUnder(service, scope, from);
            case NotWatching(String reason) -> giveUp(scope, reason);
        }
    }

    private void registerUnder(WatchService service, TreeScope scope, Path from) {
        try {
            Files.walkFileTree(from, new TreeRegistration(service, registrar, scope, watched));
        } catch (IOException e) {
            giveUp(scope, e.getMessage());
        }
    }

    private void giveUp(TreeScope scope, String reason) {
        forget(scope.root());
        polled.put(scope.root(), TreeStamps.of(scope, Map.of()));
        if (warnedOf.add(scope.root())) {
            polling.polling(scope.root(), CAUSE.formatted(scope.root(), reason), pollInterval);
        }
    }

    private void run(Runnable changed) {
        long nextPoll = System.nanoTime() + pollInterval.toNanos();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                boolean relevant = switch (state) {
                    case Watching(WatchService service) -> batch(service);
                    case NotWatching _ -> sleepThroughPoll();
                };
                if (!polled.isEmpty() && System.nanoTime() - nextPoll >= 0) {
                    nextPoll = System.nanoTime() + pollInterval.toNanos();
                    relevant |= pollTrees();
                }
                if (relevant) {
                    changed.run();
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean sleepThroughPoll() throws InterruptedException {
        Thread.sleep(pollInterval.toMillis());
        return false;
    }

    private boolean batch(WatchService service) throws InterruptedException {
        WatchKey first = service.poll(pollInterval.toMillis(), TimeUnit.MILLISECONDS);
        if (first == null) {
            return false;
        }
        boolean relevant = process(first);
        long deadline = System.nanoTime() + maxWait.toNanos();
        WatchKey more = service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS);
        while (more != null) {
            relevant |= process(more);
            more = System.nanoTime() - deadline < 0 ? service.poll(batchWindow.toMillis(), TimeUnit.MILLISECONDS) : null;
        }
        return relevant;
    }

    private boolean process(WatchKey key) {
        Path directory = (Path) key.watchable();
        Optional<TreeScope> owner = ownerOf(directory).filter(scope -> !polled.containsKey(scope.root()));
        boolean relevant = owner.map(scope -> report(scope, directory, key)).orElse(false);
        if (!key.reset()) {
            watched.remove(directory, key);
        }
        return relevant;
    }

    private boolean report(TreeScope scope, Path directory, WatchKey key) {
        return key.pollEvents().stream().map(event -> handle(scope, directory, event)).reduce(false, Boolean::logicalOr);
    }

    private Optional<TreeScope> ownerOf(Path directory) {
        return scopes.values().stream()
                .filter(scope -> directory.startsWith(scope.root()))
                .max(Comparator.comparingInt(scope -> scope.root().getNameCount()));
    }

    /** Whether {@code event} is a change to report; every event is handled, so a relevant one does not stop the rest. */
    private synchronized boolean handle(TreeScope scope, Path directory, WatchEvent<?> event) {
        if (event.kind() == OVERFLOW) {
            register(scope, scope.root());
            return true;
        }
        Path entry = directory.resolve((Path) event.context());
        if (event.kind() == ENTRY_CREATE && scope.descends(entry) && Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
            register(scope, entry);
            return true;
        }
        if (event.kind() == ENTRY_DELETE && watched.containsKey(entry)) {
            forget(entry);
            return true;
        }
        return scope.counts(entry);
    }

    /**
     * Cancels the watches on a directory that left its path and on everything beneath it: Linux keeps a moved directory's watch,
     * and a later register of its new path would hand back that key, still naming the old one.
     */
    private void forget(Path directory) {
        watched.entrySet().removeIf(watch -> {
            boolean gone = watch.getKey().startsWith(directory);
            if (gone) {
                watch.getValue().cancel();
            }
            return gone;
        });
    }

    private boolean pollTrees() {
        return polled.keySet().stream().map(this::pollTree).reduce(false, Boolean::logicalOr);
    }

    private boolean pollTree(Path root) {
        TreeScope scope = scopes.get(root);
        Map<String, String> before = polled.get(root);
        if (scope == null || before == null) {
            return false;
        }
        Map<String, String> now = TreeStamps.of(scope, before);
        polled.replace(root, before, now);
        return !now.equals(before);
    }

    private static void closeQuietly(WatchService service) {
        try {
            service.close();
        } catch (IOException ignored) {
            // a watch that will not close has nothing left to report
        }
    }
}
