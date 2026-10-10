package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.record;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.writeRecord;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.rewritingWithoutPause;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.settled;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.sweepingWatcher;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watcherWithoutSweep;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watching;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.within;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionWatchRepositoryTest {

    private static final Path CWD = Path.of("/work/app");

    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
    private final ChangeProbe probe = new ChangeProbe();
    private SessionWatchRepository watcher;

    @TempDir
    Path root;

    private Path sessions;

    @BeforeEach
    void createRegistry() throws IOException {
        sessions = root.resolve("sessions");
        writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "busy"));
    }

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldReportARecordRewrittenForAStateChange() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions, said), probe);

        writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));

        assertThat(probe.changedWithin(within())).as("without waiting for a sweep").isTrue();
    }

    @Test
    void shouldReportANewRecord() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions, said), probe);

        writeRecord(sessions, "22862.json", record(22862, "a3d7d4b5-4cdb-479e-a82e-622d2542a6b8", CWD, "busy"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADeletedRecord() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions, said), probe);

        Files.delete(sessions.resolve("17234.json"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldIgnoreAKeyFile() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions, said), probe);

        writeRecord(sessions, "17234.0123abcd.key", "secret");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportOnceForOneSaveThatFiresSeveralEvents() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions, said), probe);

        writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));
        assertThat(probe.changedWithin(within())).isTrue();

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportARecordWhileAnotherFileIsRewrittenWithoutPause() throws Exception {
        watcher = watching(sweepingWatcher(sessions, said), probe);

        try (AutoCloseable busy = rewritingWithoutPause(sessions.resolve("17234.0123abcd.key"))) {
            writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));

            assertThat(probe.changedWithin(within()))
                    .as("a batch that never closes would hold back every state change and every sweep")
                    .isTrue();
        }
    }

    @Test
    void shouldSweepEvenWhenNothingIsRewritten() throws Exception {
        watcher = watching(sweepingWatcher(sessions, said), probe);

        assertThat(probe.changedWithin(within()))
                .as("a process that died never rewrites its record")
                .isTrue();
        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWarnOnceNamingTheDirectoryWhenTheWatchCannotBeSetUp() {
        watcher = watching(watcherWithoutInotify(sessions, said), probe);

        assertThat(said.toString(StandardCharsets.UTF_8))
                .contains(sessions.toString())
                .contains("User limit of inotify watches reached")
                .hasLineCount(1);
    }

    @Test
    void shouldLeaveTheSweepCarryingChangesWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(sessions, said), probe);

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWarnAndSweepWhenTheRegistryDirectoryDoesNotExist() throws Exception {
        watcher = watching(sweepingWatcher(root.resolve("absent"), said), probe);

        assertThat(said.toString(StandardCharsets.UTF_8)).contains("absent").hasLineCount(1);
        assertThat(probe.changedWithin(within())).isTrue();
    }
}
