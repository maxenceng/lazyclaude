package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.PREFERRED_FOLDER;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.TMPDIR;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.bundleProperties;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.lockFileIn;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.lockedInThisProcess;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.nativeFolderUnder;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.noProperties;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyNativeFolderFixture.toldReasons;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.HostFallback;

class PtyNativeFolderTest {

    @TempDir
    Path cache;

    private final HostFallback fallback = mock(HostFallback.class);
    private final Properties properties = noProperties();

    @Test
    void shouldCreateTheFolderAndPointPty4jAtIt() {
        Path folder = nativeFolderUnder(cache);

        PtyNativeFolder.prepared(folder, properties).loading(() -> true, fallback);

        assertThat(folder).as("pty4j falls back to the temp folder when pty4j.tmpdir does not exist").isDirectory();
        assertThat(properties.getProperty(TMPDIR)).isEqualTo(folder.toString());
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldReuseAFolderAnEarlierRunCreated() throws Exception {
        Path folder = Files.createDirectories(nativeFolderUnder(cache));

        PtyNativeFolder.prepared(folder, properties).loading(() -> true, fallback);

        assertThat(properties.getProperty(TMPDIR)).isEqualTo(folder.toString());
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldLeaveTheTmpdirUnsetWhenTheBundleNamesItsOwnNatives() {
        Properties bundle = bundleProperties();
        Path folder = nativeFolderUnder(cache);

        PtyNativeFolder.prepared(folder, bundle).loading(() -> true, fallback);

        assertThat(bundle.getProperty(TMPDIR)).as("the bundle's pre-extracted natives are used as they are (ADR 0059)").isNull();
        assertThat(bundle.getProperty(PREFERRED_FOLDER)).isNotNull();
        assertThat(folder).doesNotExist();
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldTellTheFirstSpawnTheFolderAndTheReasonWhenAFileIsInTheWay() throws Exception {
        Files.writeString(cache.resolve("lazyclaude"), "not a folder");
        Path folder = nativeFolderUnder(cache);

        PtyNativeFolder.prepared(folder, properties).loading(() -> true, fallback);

        assertThat(properties.getProperty(TMPDIR)).as("pty4j extracts into the temp folder rather than not at all").isNull();
        assertThat(toldReasons(fallback))
                .singleElement()
                .asString()
                .contains(folder.toString())
                .contains("Not a directory");
    }

    @Test
    void shouldRunTheFirstLoadUnderTheFoldersLock() throws Exception {
        Path folder = nativeFolderUnder(cache);
        PtyNativeFolder natives = PtyNativeFolder.prepared(folder, properties);

        assertThat(natives.loading(() -> lockedInThisProcess(folder), fallback))
                .as("pty4j extracts on the first spawn, so another process must not extract at the same time")
                .isTrue();
        assertThat(lockedInThisProcess(folder)).as("the lock is released once the load returns").isFalse();
    }

    @Test
    void shouldLoadWithoutTheLockOnceALoadSucceeded() throws Exception {
        Path folder = nativeFolderUnder(cache);
        PtyNativeFolder natives = PtyNativeFolder.prepared(folder, properties);
        natives.loading(() -> true, fallback);

        assertThat(natives.loading(() -> lockedInThisProcess(folder), fallback))
                .as("pty4j extracts once per process, so later spawns need not wait on another process")
                .isFalse();
    }

    @Test
    void shouldLockAgainAfterAFirstLoadThatFailed() throws Exception {
        Path folder = nativeFolderUnder(cache);
        PtyNativeFolder natives = PtyNativeFolder.prepared(folder, properties);

        assertThatThrownBy(() -> natives.loading(() -> {
                            throw new IllegalStateException("Cannot extract pty4j native linux/x86-64");
                        },
                        fallback))
                .isInstanceOf(IllegalStateException.class);
        assertThat(natives.loading(() -> lockedInThisProcess(folder), fallback))
                .as("a load that threw may not have extracted, so the next one still extracts under the lock")
                .isTrue();
    }

    @Test
    void shouldTellTheFirstSpawnAndLeaveTheTmpdirUnsetWhenTheLockCannotBeTaken() throws Exception {
        Path folder = nativeFolderUnder(cache);
        Files.createDirectories(lockFileIn(folder));

        PtyNativeFolder.prepared(folder, properties).loading(() -> true, fallback);

        assertThat(properties.getProperty(TMPDIR))
                .as("a folder whose lock cannot be taken would let two processes extract into it at once")
                .isNull();
        assertThat(toldReasons(fallback)).singleElement().asString().contains(folder.toString());
    }

    @Test
    void shouldStillLoadWhenTheFolderWentAwayAfterItWasPrepared() throws Exception {
        Path folder = nativeFolderUnder(cache);
        PtyNativeFolder natives = PtyNativeFolder.prepared(folder, properties);
        Files.delete(lockFileIn(folder));
        Files.delete(folder);

        assertThat(natives.loading(() -> true, fallback))
                .as("a folder that cannot be used does not stop sessions from starting (ADR 0114)")
                .isTrue();
        assertThat(toldReasons(fallback))
                .as("the temp-folder leak is back for this run, so it is said")
                .singleElement()
                .asString()
                .contains(folder.toString());
    }

    @Test
    void shouldTellAnUnusableFolderOnceAcrossSpawns() throws Exception {
        Files.writeString(cache.resolve("lazyclaude"), "not a folder");
        PtyNativeFolder natives = PtyNativeFolder.prepared(nativeFolderUnder(cache), properties);

        natives.loading(() -> true, fallback);
        natives.loading(() -> true, fallback);

        assertThat(toldReasons(fallback)).as("a degraded mode is said once, not at every spawn").hasSize(1);
    }

    @Test
    void shouldTellNothingBeforeAnyLoad() throws Exception {
        Files.writeString(cache.resolve("lazyclaude"), "not a folder");

        PtyNativeFolder.prepared(nativeFolderUnder(cache), properties);

        verifyNoInteractions(fallback);
    }

    @Test
    void shouldLoadWithoutALockWhenTheFolderCouldNotBeCreated() throws Exception {
        Files.writeString(cache.resolve("lazyclaude"), "not a folder");
        PtyNativeFolder natives = PtyNativeFolder.prepared(nativeFolderUnder(cache), properties);

        assertThat(natives.loading(() -> true, fallback)).as("sessions still start, extracting into the temp folder").isTrue();
    }
}
