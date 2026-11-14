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
 * {@code deleteOnExit} cannot remove once it holds a file (ADR 0113). pty4j extracts on the first spawn of a process and checks
 * an existing extraction against its own, deleting and rewriting it on a mismatch, so that first spawn runs under an exclusive
 * file lock that another lazyclaude starting its first session waits on.
 */
public final class PtyNativeFolder {

    private static final String TMPDIR = "pty4j.tmpdir";
    private static final String PREFERRED_FOLDER = "pty4j.preferred.native.folder";
    private static final String LOCK_FILE = "extract.lock";
    private static final String UNUSABLE =
            "sessions: cannot keep pty4j's natives in %s (%s); each run leaves a pty4j folder in the temp folder";
    private static final String UNLOCKABLE =
            "sessions: cannot lock pty4j's natives in %s (%s); two lazyclaude starting a first session at once may not extract them";
    private static final String UNRELEASED = "sessions: cannot release pty4j's natives lock in %s (%s)";

    private final Object lock = new Object();
    private final Optional<Path> lockFile;
    private final WarningPort warnings;
    private boolean loaded;

    private PtyNativeFolder(Optional<Path> lockFile, WarningPort warnings) {
        this.lockFile = lockFile;
        this.warnings = warnings;
    }

    /**
     * Creates {@code folder} and its lock file and points pty4j at it through {@code properties}, unless they already name
     * natives of their own. A folder that cannot be created is named in a warning and left out, so pty4j falls back to the temp
     * folder rather than sessions not starting.
     */
    public static PtyNativeFolder prepared(Path folder, WarningPort warnings, Properties properties) {
        if (properties.getProperty(PREFERRED_FOLDER) != null) {
            return new PtyNativeFolder(Optional.empty(), warnings);
        }
        try {
            Files.createDirectories(folder);
            Path lockFile = folder.resolve(LOCK_FILE);
            opened(lockFile).close();
            properties.setProperty(TMPDIR, folder.toString());
            return new PtyNativeFolder(Optional.of(lockFile), warnings);
        } catch (IOException unusable) {
            warnings.raise(new Warning(UNUSABLE.formatted(folder, reason(unusable))));
            return new PtyNativeFolder(Optional.empty(), warnings);
        }
    }

    /**
     * Runs {@code load}, under the folder's lock until one load has returned: pty4j extracts once per process, so later spawns
     * neither wait on another process nor make it wait. A lock that cannot be taken is named in a warning and {@code load}
     * runs without it, so sessions still start.
     */
    <T> T loading(Supplier<T> load) {
        synchronized (lock) {
            if (loaded) {
                return load.get();
            }
            T result = lockFile.map(file -> locked(file, load, warnings)).orElseGet(load);
            loaded = true;
            return result;
        }
    }

    private static <T> T locked(Path lockFile, Supplier<T> load, WarningPort warnings) {
        Optional<FileChannel> held = held(lockFile, warnings);
        try {
            return load.get();
        } finally {
            held.ifPresent(channel -> released(channel, lockFile, warnings));
        }
    }

    /** The lock file's channel, holding its exclusive lock, or none when it cannot be taken, which a warning then names. */
    private static Optional<FileChannel> held(Path lockFile, WarningPort warnings) {
        try {
            FileChannel channel = opened(lockFile);
            try {
                channel.lock();
                return Optional.of(channel);
            } catch (IOException unlockable) {
                channel.close();
                throw unlockable;
            }
        } catch (IOException unlockable) {
            warnings.raise(new Warning(UNLOCKABLE.formatted(lockFile.getParent(), reason(unlockable))));
            return Optional.empty();
        }
    }

    /** Closes {@code channel}, which releases its lock; a close that fails is named, the load it guarded having returned. */
    private static void released(FileChannel channel, Path lockFile, WarningPort warnings) {
        try {
            channel.close();
        } catch (IOException unclosable) {
            warnings.raise(new Warning(UNRELEASED.formatted(lockFile.getParent(), reason(unclosable))));
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
