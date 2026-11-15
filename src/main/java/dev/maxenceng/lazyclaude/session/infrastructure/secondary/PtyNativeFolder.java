package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

/**
 * Where pty4j extracts its natives, kept across runs: pty4j's own default is a new temp folder per process that its
 * {@code deleteOnExit} cannot remove once it holds a file (ADR 0114). pty4j extracts on the first spawn of a process and checks
 * an existing extraction against its own, deleting and rewriting it on a mismatch, so that first spawn runs under an exclusive
 * file lock that another lazyclaude starting its first session waits on.
 */
public final class PtyNativeFolder {

    private static final String TMPDIR = "pty4j.tmpdir";
    private static final String PREFERRED_FOLDER = "pty4j.preferred.native.folder";
    private static final String LOCK_FILE = "extract.lock";
    private static final String UNUSABLE =
            "sessions: cannot keep pty4j's natives in %s (%s); each run leaves a pty4j folder in the temp folder";

    private final Object lock = new Object();
    private final Optional<Path> lockFile;
    private boolean loaded;

    private PtyNativeFolder(Optional<Path> lockFile) {
        this.lockFile = lockFile;
    }

    /**
     * Creates {@code folder} and its lock file, takes the lock once to learn it can be taken, and points pty4j at the folder
     * through {@code properties}, unless they already name natives of their own. A folder that cannot be created or locked is
     * named in a warning and left out, so pty4j falls back to the temp folder rather than sessions not starting.
     */
    public static PtyNativeFolder prepared(Path folder, WarningPort warnings, Properties properties) {
        if (properties.getProperty(PREFERRED_FOLDER) != null) {
            return new PtyNativeFolder(Optional.empty());
        }
        try {
            Files.createDirectories(folder);
            Path lockFile = folder.resolve(LOCK_FILE);
            try (FileChannel channel = opened(lockFile)) {
                channel.lock();
            }
            properties.setProperty(TMPDIR, folder.toString());
            return new PtyNativeFolder(Optional.of(lockFile));
        } catch (IOException unusable) {
            warnings.raise(new Warning(UNUSABLE.formatted(folder, reason(unusable))));
            return new PtyNativeFolder(Optional.empty());
        }
    }

    /**
     * Runs {@code load}, under the folder's lock until one load has returned: pty4j extracts once per process, so later spawns
     * neither wait on another process nor make it wait.
     */
    <T> T loading(Supplier<T> load) {
        synchronized (lock) {
            if (loaded) {
                return load.get();
            }
            T result = lockFile.map(file -> locked(file, load)).orElseGet(load);
            loaded = true;
            return result;
        }
    }

    /**
     * Runs {@code load} under the lock. The lock was taken once when prepared, so failing now means the folder went away
     * meanwhile, in which case pty4j extracts into a temp folder of its own and there is nothing shared left to guard.
     */
    private static <T> T locked(Path lockFile, Supplier<T> load) {
        Optional<FileChannel> held = held(lockFile);
        try {
            return load.get();
        } finally {
            held.ifPresent(PtyNativeFolder::released);
        }
    }

    /** The lock file's channel holding its exclusive lock, or none when the folder has gone. */
    private static Optional<FileChannel> held(Path lockFile) {
        // deferred: a folder removed while running brings the temp-folder leak back for that run, unwarned — a report of it
        try {
            FileChannel channel = opened(lockFile);
            try {
                channel.lock();
                return Optional.of(channel);
            } catch (IOException unlockable) {
                channel.close();
                throw unlockable;
            }
        } catch (IOException gone) {
            return Optional.empty();
        }
    }

    /** Closes {@code channel}; its descriptor, and with it the lock, is gone whether or not the close reports a failure. */
    private static void released(FileChannel channel) {
        try {
            channel.close();
        } catch (IOException closedAnyway) {
            Objects.requireNonNull(closedAnyway);
        }
    }

    /** Opens {@code lockFile} for locking, creating it if absent, which another process starting at once may also be doing. */
    private static FileChannel opened(Path lockFile) throws IOException {
        return FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /** What went wrong without the path, which the sentence around it already names. */
    private static String reason(IOException failure) {
        String stated = failure instanceof FileSystemException onPath ? onPath.getReason() : failure.getMessage();
        return Objects.requireNonNullElse(stated, failure.getClass().getSimpleName());
    }
}
