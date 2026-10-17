package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedInProject;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.SessionWatchFixture.within;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.append;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.assistantLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.cwd;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.folder;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.later;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.latest;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.otherSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.started;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.userLine;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptFixture.writeTranscript;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.longPollingWatcherWithoutInotify;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.pollTime;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.quietLongerThanTheSettleTime;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.repositoryOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.watcherLosingEvents;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.watcherOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.TranscriptWatchFixture.watcherWithoutInotify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.PollingFallback;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.TranscriptLocator;
import dev.maxenceng.lazyclaude.session.published.Reason;

@ExtendWith(MockitoExtension.class)
class TranscriptWatchRepositoryTest {

    @Mock
    private TranscriptLocator locator;

    @Mock
    private PollingFallback fallback;

    @Mock
    private CacheFallback cache;

    @Mock
    private RootFallback unlistable;

    private final ChangeProbe probe = new ChangeProbe();
    private TranscriptRepository transcripts;
    private TranscriptWatchRepository watcher;

    @TempDir
    Path root;

    private Path projects;

    @BeforeEach
    void setUp() {
        projects = root.resolve("projects");
        transcripts = repositoryOver(projects, root.resolve("cache/past-sessions.json"));
        lenient().when(locator.locate(any())).thenReturn(locatedInProject());
    }

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldReadTheTranscriptsAtStartAndSayWhenTheyHaveLanded() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);

        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions()).as("read off the caller's thread, and landed before the callback").hasSize(1);
    }

    @Test
    void shouldReportANewTranscriptInAFolderThatIsWatched() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        writeTranscript(projects, folder(), otherSessionId(), userLine(cwd(), later(), "Another"));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions()).hasSize(2);
    }

    @Test
    void shouldReportATranscriptInAFolderCreatedLater() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        writeTranscript(projects, "-work-new-folder", otherSessionId(), userLine(cwd(), later(), "Another"));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions())
                .as("the folder did not exist when the watch was set up")
                .hasSize(2);
    }

    @Test
    void shouldReadAGrowingTranscriptOnceItHasBeenQuietAndNotOnEveryLine() throws Exception {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        for (int line = 0; line < 5; line++) {
            append(file, assistantLine(cwd(), later()));
            Thread.sleep(50);
        }

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(quietLongerThanTheSettleTime()))
                .as("five lines in a quarter of a second are one read, not five")
                .isTrue();
        assertThat(transcripts.pastSessions()).singleElement().satisfies(session -> assertThat(session.session().lastActivity().value()).isEqualTo(Instant.parse(later())));
    }

    @Test
    void shouldReportADeletedTranscriptAndForgetIt() throws Exception {
        Path file = writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        Files.delete(file);

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions()).isEmpty();
    }

    @Test
    void shouldRestateEveryTranscriptOnAnOverflowWhenItsEventsWereLost() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        LostEventsPoll keys = new LostEventsPoll(projects);
        watcher = watcherLosingEvents(projects, transcripts, keys);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();
        writeTranscript(projects, folder(), otherSessionId(), userLine(cwd(), later(), "Another"));
        assertThat(probe.quietFor(quietLongerThanTheSettleTime()))
                .as("its create event was dropped, so nothing has read it yet")
                .isTrue();

        keys.overflow();

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions())
                .as("found by re-stating every transcript, with no event naming it")
                .hasSize(2);
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldWatchAFolderWhoseCreationWasLostInAnOverflow() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        LostEventsPoll keys = new LostEventsPoll(projects);
        watcher = watcherLosingEvents(projects, transcripts, keys);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();
        writeTranscript(projects, "-work-new-folder", otherSessionId(), userLine(cwd(), later(), "Another"));
        keys.overflow();
        assertThat(probe.changedWithin(within())).isTrue();

        writeTranscript(projects, "-work-new-folder", thirdSessionId(), userLine(cwd(), latest(), "A third"));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(transcripts.pastSessions())
                .as("the folder's create event was lost, so only the overflow could have put a watch on it")
                .hasSize(3);
    }

    @Test
    void shouldSayNothingAndWatchNothingWhenTheProjectsDirectoryIsMissing() throws Exception {
        watcher = watcherOver(projects, transcripts);

        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);

        assertThat(probe.changedWithin(within())).as("it still tells the domain the (empty) scan landed").isTrue();
        assertThat(probe.quietFor(quietLongerThanTheSettleTime())).isTrue();
        verifyNoInteractions(fallback, cache);
    }

    @Test
    void shouldRestateEveryTranscriptOnATimerAndWarnOnceWhenTheWatchCannotBeSetUp() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherWithoutInotify(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        writeTranscript(projects, folder(), otherSessionId(), userLine(cwd(), later(), "Another"));

        assertThat(probe.changedWithin(within())).as("found by the timer, with no event to go on").isTrue();
        assertThat(transcripts.pastSessions()).hasSize(2);
        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).polling(cause.capture(), eq(pollTime()));
        assertThat(cause.getValue().value())
                .contains(projects.toString(), "max_user_watches", "User limit of inotify watches reached");
    }

    @Test
    void shouldReadNothingAtStartWhenTheWatchCannotBeSetUpAndTheDirectoryIsMissing() throws IOException, InterruptedException {
        watcher = watcherWithoutInotify(projects, transcripts);

        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);

        assertThat(probe.changedWithin(within())).isTrue();
        verifyNoInteractions(fallback);
    }

    @Test
    void shouldReadEveryTranscriptAgainAndReportWhenAskedToRescanNow() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = watcherOver(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(quietLongerThanTheSettleTime())).isTrue();

        watcher.rescanNow();

        assertThat(probe.changedWithin(within()))
                .as("nothing on disk changed, so only the request can have made the watcher report")
                .isTrue();
    }

    @Test
    void shouldReadEveryTranscriptAgainAtOnceWhenAskedToRescanNowWhileItPollsOnATimer() throws Exception {
        writeTranscript(projects, folder(), sessionId(), userLine(cwd(), started(), "Refine"));
        watcher = longPollingWatcherWithoutInotify(projects, transcripts);
        watcher.watch(locator, probe.listener(), fallback, cache, unlistable);
        assertThat(probe.changedWithin(within())).isTrue();

        watcher.rescanNow();

        assertThat(probe.changedWithin(within())).as("the timer is a minute, so the wait was cut short").isTrue();
    }
}
