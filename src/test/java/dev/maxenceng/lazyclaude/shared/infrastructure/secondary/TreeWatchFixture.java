package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public final class TreeWatchFixture {

    private static final DirectoryRegistrar REAL_REGISTRAR =
            (service, directory) -> directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    private static final Duration SHORT_BATCH = Duration.ofMillis(150);
    private static final Duration SHORT_MAX_WAIT = Duration.ofMillis(600);
    private static final Duration SHORT_POLL = Duration.ofMillis(200);
    private static final long STREAM_PAUSE_MILLIS = 50;

    private TreeWatchFixture() {}

    public static Duration within() {
        return Duration.ofSeconds(5);
    }

    public static Duration pollInterval() {
        return SHORT_POLL;
    }

    public static Duration maxWait() {
        return SHORT_MAX_WAIT;
    }

    public static Duration settled() {
        return Duration.ofMillis(700);
    }

    public static String markdown() {
        return "# Context map\n\nEach context is a package.\n";
    }

    public static Path write(Path tree, String relative, String text) throws IOException {
        Path file = tree.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text);
    }

    public static void deleteTree(Path tree) throws IOException {
        try (Stream<Path> paths = Files.walk(tree)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    public static Thread steadyWrites(Path tree) {
        return Thread.ofPlatform().daemon().start(() -> {
            try {
                int save = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    write(tree, "stream-%d.md".formatted(save++ % 3), markdown());
                    Thread.sleep(STREAM_PAUSE_MILLIS);
                }
            } catch (IOException | InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
        });
    }

    static TreeScope markdownUnder(Path root) {
        return new MarkdownScope(root, Set.of());
    }

    static TreeScope markdownUnderExcept(Path root, Path skipped) {
        return new MarkdownScope(root, Set.of(skipped));
    }

    static TreeWatcher started(TreeWatcher watcher, ChangeProbe probe, TreePolling polling) {
        watcher.start(probe.listener(), polling);
        return watcher;
    }

    public static TreeWatcher watcher() {
        return new TreeWatcher(
                () -> FileSystems.getDefault().newWatchService(), REAL_REGISTRAR, SHORT_BATCH, SHORT_MAX_WAIT, SHORT_POLL);
    }

    public static TreeWatcher watcherWithoutInotify() {
        return new TreeWatcher(
                () -> {
                    throw new IOException("User limit of inotify watches reached");
                },
                REAL_REGISTRAR,
                SHORT_BATCH,
                SHORT_MAX_WAIT,
                SHORT_POLL);
    }

    static TreeWatcher watcherOver(FakeWatchService service, DirectoryRegistrar registrar) {
        return new TreeWatcher(() -> service, registrar, SHORT_BATCH, SHORT_MAX_WAIT, SHORT_POLL);
    }

    static TreeWatcher watcherRegisteringWith(DirectoryRegistrar registrar) {
        return new TreeWatcher(
                () -> FileSystems.getDefault().newWatchService(), registrar, SHORT_BATCH, SHORT_MAX_WAIT, SHORT_POLL);
    }
}
