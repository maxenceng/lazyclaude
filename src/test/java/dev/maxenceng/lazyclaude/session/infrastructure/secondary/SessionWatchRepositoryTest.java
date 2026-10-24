package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.record;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.RegistryFixture.writeRecord;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.rewritingWithoutPause;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.settled;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.sweepInterval;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.sweepingWatcher;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watcherWithoutSweep;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.watching;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.WatchFallback;
import dev.maxenceng.lazyclaude.session.published.Reason;

@ExtendWith(MockitoExtension.class)
class SessionWatchRepositoryTest {

    private static final Path CWD = Path.of("/work/app");

    @Mock
    private WatchFallback fallback;

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
        watcher = watching(watcherWithoutSweep(sessions), probe, fallback);

        writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));

        assertThat(probe.changedWithin(within())).as("without waiting for a sweep").isTrue();
    }

    @Test
    void shouldReportANewRecord() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions), probe, fallback);

        writeRecord(sessions, "22862.json", record(22862, "a3d7d4b5-4cdb-479e-a82e-622d2542a6b8", CWD, "busy"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADeletedRecord() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions), probe, fallback);

        Files.delete(sessions.resolve("17234.json"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldIgnoreAKeyFile() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions), probe, fallback);

        writeRecord(sessions, "17234.0123abcd.key", "secret");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportOnceForOneSaveThatFiresSeveralEvents() throws Exception {
        watcher = watching(watcherWithoutSweep(sessions), probe, fallback);

        writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));
        assertThat(probe.changedWithin(within())).isTrue();

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldReportARecordWhileAnotherFileIsRewrittenWithoutPause() throws Exception {
        watcher = watching(sweepingWatcher(sessions), probe, fallback);

        try (AutoCloseable _ = rewritingWithoutPause(sessions.resolve("17234.0123abcd.key"))) {
            writeRecord(sessions, "17234.json", record(17234, "70c67de2-4563-47c2-b946-eb5198915ba2", CWD, "idle"));

            assertThat(probe.changedWithin(within()))
                    .as("a batch that never closes would hold back every state change and every sweep")
                    .isTrue();
        }
    }

    @Test
    void shouldSweepEvenWhenNothingIsRewritten() throws Exception {
        watcher = watching(sweepingWatcher(sessions), probe, fallback);

        assertThat(probe.changedWithin(within()))
                .as("a process that died never rewrites its record")
                .isTrue();
        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWarnOnceNamingTheDirectoryWhenTheWatchCannotBeSetUp() {
        watcher = watching(watcherWithoutInotify(sessions), probe, fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).sweepingAlone(cause.capture(), eq(sweepInterval()));
        assertThat(cause.getValue().value()).contains(sessions.toString()).contains("User limit of inotify watches reached");
    }

    @Test
    void shouldLeaveTheSweepCarryingChangesWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(sessions), probe, fallback);

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWarnAndSweepWhenTheRegistryDirectoryDoesNotExist() throws Exception {
        watcher = watching(sweepingWatcher(root.resolve("absent")), probe, fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).sweepingAlone(cause.capture(), any());
        assertThat(cause.getValue().value()).contains("absent");
        assertThat(probe.changedWithin(within())).isTrue();
    }
}
