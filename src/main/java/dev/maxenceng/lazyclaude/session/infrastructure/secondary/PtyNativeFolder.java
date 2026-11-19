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

import dev.maxenceng.lazyclaude.session.domain.HostFallback;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * Where pty4j extracts its natives, kept across runs: pty4j's own default is a new temp folder per process that its
 * {@code deleteOnExit} cannot remove once it holds a file (ADR 0114). pty4j extracts on the first spawn of a process and checks
 * an existing extraction against its own, deleting and rewriting it on a mismatch, so that first spawn runs under an exclusive
 * file lock that another lazyclaude starting its first session waits on. A folder that cannot be used is told to the first
 * spawn's {@link HostFallback}, once, and the domain words the warning.
 */
public final class PtyNativeFolder {

    private static final String TMPDIR = "pty4j.tmpdir";
    private static final String PREFERRED_FOLDER = "pty4j.preferred.native.folder";
    private static final String LOCK_FILE = "extract.lock";
    private static final String UNUSABLE = "cannot keep pty4j's natives in %s (%s)";
    private static final String GONE = "pty4j's natives folder %s went away (%s)";

    private final Object lock = new Object();
    private final Path folder;
    private final Optional<Path> lockFile;
    private Optional<Reason> unusable;
    private boolean loaded;

    private PtyNativeFolder(Path folder, Optional<Path> lockFile, Optional<Reason> unusable) {
        this.folder = folder;
        this.lockFile = lockFile;
        this.unusable = unusable;
    }

    /**
     * Creates {@code folder} and its lock file, takes the lock once to learn it can be taken, and points pty4j at the folder
     * through {@code properties}, unless they already name natives of their own. A folder that cannot be created or locked is
     * left out, so pty4j falls back to the temp folder rather than sessions not starting, and kept for the first spawn to tell.
     */
    public static PtyNativeFolder prepared(Path folder, Properties properties) {
        if (properties.getProperty(PREFERRED_FOLDER) != null) {
            return new PtyNativeFolder(folder, Optional.empty(), Optional.empty());
        }
        try {
            Files.createDirectories(folder);
            Path lockFile = folder.resolve(LOCK_FILE);
            try (FileChannel channel = opened(lockFile)) {
                channel.lock();
            }
            properties.setProperty(TMPDIR, folder.toString());
            return new PtyNativeFolder(folder, Optional.of(lockFile), Optional.empty());
        } catch (IOException unusable) {
            return new PtyNativeFolder(
                    folder, Optional.empty(), Optional.of(new Reason(UNUSABLE.formatted(folder, reason(unusable)))));
        }
    }

    /**
     * Runs {@code load}, under the folder's lock until one load has returned: pty4j extracts once per process, so later spawns
     * neither wait on another process nor make it wait. A folder that could not be used is told to {@code fallback}, once.
     */
    <T> T loading(Supplier<T> load, HostFallback fallback) {
        synchronized (lock) {
            if (loaded) {
                return load.get();
            }
            T result = lockFile.map(file -> locked(file, load)).orElseGet(load);
            loaded = true;
            unusable.ifPresent(fallback::leaking);
            unusable = Optional.empty();
            return result;
        }
    }

    /**
     * Runs {@code load} under the lock. The lock was taken once when prepared, so failing now means the folder went away
     * meanwhile, in which case pty4j extracts into a temp folder of its own and there is nothing shared left to guard.
     */
    private <T> T locked(Path file, Supplier<T> load) {
        Optional<FileChannel> held = held(file);
        try {
            return load.get();
        } finally {
            held.ifPresent(PtyNativeFolder::released);
        }
    }

    /** The lock file's channel holding its exclusive lock, or none when the folder has gone, which is kept for the fallback. */
    private Optional<FileChannel> held(Path file) {
        try {
            FileChannel channel = opened(file);
            try {
                channel.lock();
                return Optional.of(channel);
            } catch (IOException unlockable) {
                channel.close();
                throw unlockable;
            }
        } catch (IOException gone) {
            unusable = Optional.of(new Reason(GONE.formatted(folder, reason(gone))));
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
