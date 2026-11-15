package dev.maxenceng.lazyclaude.session.infrastructure.secondary;


import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;

import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.session.domain.HostFallback;
import dev.maxenceng.lazyclaude.session.published.Reason;


public final class PtyNativeFolderFixture {

    public static final String TMPDIR = "pty4j.tmpdir";
    public static final String PREFERRED_FOLDER = "pty4j.preferred.native.folder";

    private static final PtyNativeFolder TEST_RUN_NATIVES = PtyNativeFolder.prepared(
            Path.of("target", "pty4j").toAbsolutePath(), System.getProperties());

    private PtyNativeFolderFixture() {}

    public static PtyNativeFolder testRunNatives() {
        return TEST_RUN_NATIVES;
    }

    public static Path nativeFolderUnder(Path cache) {
        return cache.resolve("lazyclaude").resolve("pty4j");
    }

    public static Properties noProperties() {
        return new Properties();
    }

    public static Properties bundleProperties() {
        Properties properties = new Properties();
        properties.setProperty(PREFERRED_FOLDER, "/usr/lib/lazyclaude/natives");
        return properties;
    }

    public static Path lockFileIn(Path folder) {
        return folder.resolve("extract.lock");
    }

    public static boolean lockedInThisProcess(Path folder) {
        try (FileChannel channel = FileChannel.open(lockFileIn(folder), StandardOpenOption.WRITE)) {
            FileLock lock = channel.tryLock();
            lock.release();
            return false;
        } catch (OverlappingFileLockException held) {
            return true;
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    public static List<String> toldReasons(HostFallback fallback) {
        ArgumentCaptor<Reason> told = ArgumentCaptor.forClass(Reason.class);
        verify(fallback, atLeast(0)).leaking(told.capture());
        return told.getAllValues().stream().map(Reason::value).toList();
    }
}
