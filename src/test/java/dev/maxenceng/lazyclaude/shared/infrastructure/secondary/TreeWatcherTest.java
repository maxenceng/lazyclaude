package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.deleteTree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.markdown;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.markdownUnder;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.markdownUnderExcept;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.maxWait;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.pollInterval;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.settled;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.started;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.steadyWrites;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcher;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcherOver;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcherRegisteringWith;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.within;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

@ExtendWith(MockitoExtension.class)
class TreeWatcherTest {

    @Mock
    private TreePolling polling;

    private final ChangeProbe probe = new ChangeProbe();
    private TreeWatcher watcher;

    @TempDir
    Path tree;

    @BeforeEach
    void createTree() throws IOException {
        write(tree, "glossary.md", markdown());
        write(tree, "adr/0001-record.md", markdown());
    }

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldReportOnceWithinTheMaximumWaitUnderAStreamThatNeverGoesQuiet() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));
        Thread writer = steadyWrites(tree);

        boolean reported = probe.changedWithin(maxWait().plus(maxWait()));
        writer.interrupt();

        assertThat(reported)
                .as("the batch is closed at the maximum wait, so a save every half window still gets read")
                .isTrue();
    }

    @Test
    void shouldReportASavedFile() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "glossary.md", "# Glossary\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportAnAddedFile() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "new.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADeletedFile() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.delete(tree.resolve("adr/0001-record.md"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADirectoryMovedOutOfTheTree(@TempDir Path elsewhere) throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.move(tree.resolve("adr"), elsewhere.resolve("adr"));

        assertThat(probe.changedWithin(within()))
                .as("a directory moved away fires one delete for the directory and none for the files in it")
                .isTrue();
    }

    @Test
    void shouldReportOnceForABurstOfSaves() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        for (int save = 0; save < 5; save++) {
            write(tree, "burst-%d.md".formatted(save), markdown());
        }

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled()))
                .as("one editor save fires several events, and the tree is read once for them")
                .isTrue();
    }

    @Test
    void shouldWatchADirectoryAddedAfterItStarted() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.createDirectories(tree.resolve("research"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(tree, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("a file saved in the new directory shows up without a restart")
                .isTrue();
    }

    @Test
    void shouldWatchADirectoryNestedInAnAddedDirectory() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.createDirectories(tree.resolve("research/old"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(tree, "research/old/a.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWatchADirectoryAddedInsideARenamedDirectory() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.move(tree.resolve("adr"), tree.resolve("decisions"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        Files.createDirectories(tree.resolve("decisions/drafts"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(tree, "decisions/drafts/a.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("the renamed directory's watch still names its old path, so the directory added in it was never watched")
                .isTrue();
    }

    @Test
    void shouldIgnoreAChangeUnderADirectoryTheScopeDoesNotDescendInto() throws Exception {
        Files.createDirectories(tree.resolve("backlog"));
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnderExcept(tree, tree.resolve("backlog")));

        write(tree, "backlog/ticket.md", markdown());
        write(tree, "backlog/nested/PROJ-2.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldIgnoreAChangeToAFileTheScopeDoesNotCount() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "adr/.0001-record.md.swp", "x");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldWatchTwoTreesAtOnce(@TempDir Path other) throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));
        watcher.add(markdownUnder(other));

        write(other, "a.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldStopReportingATreeThatWasDropped(@TempDir Path other) throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(other));
        watcher.drop(other);

        write(other, "a.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
        assertThat(watcher.covers(other)).isFalse();
    }

    @Test
    void shouldNotWatchARootThatIsNotADirectory() throws Exception {
        watcher = started(watcher(), probe, polling);

        watcher.add(markdownUnder(tree.resolve("missing")));

        assertThat(watcher.covers(tree.resolve("missing"))).isFalse();
        verifyNoInteractions(polling);
    }

    @Test
    void shouldSayNothingWhenTheWatchIsSetUp() {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        verifyNoInteractions(polling);
    }

    @Test
    void shouldWarnOnceNamingTheTreeAndTheSysctlWhenTheWatchCannotBeSetUp() {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        ArgumentCaptor<String> cause = ArgumentCaptor.forClass(String.class);
        verify(polling).polling(eq(tree), cause.capture(), eq(pollInterval()));
        assertThat(cause.getValue())
                .contains(tree.toString())
                .contains("User limit of inotify watches reached")
                .contains("fs.inotify.max_user_watches");
    }

    @Test
    void shouldPollForChangesWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldPollForAnAddedFileInANewDirectory() throws Exception {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldNotWarnAgainWhileItKeepsPolling() throws Exception {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "one.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();
        write(tree, "two.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();

        verify(polling, times(1)).polling(any(), any(), any());
    }

    @Test
    void shouldNotWarnAgainForATreeDroppedAndAddedAgain() {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        watcher.drop(tree);
        watcher.add(markdownUnder(tree));

        verify(polling, times(1)).polling(any(), any(), any());
        assertThat(watcher.covers(tree)).isTrue();
    }

    @Test
    void shouldIgnoreAnEventQueuedBeforeItsTreeWasGivenUpToPolling() throws Exception {
        FakeWatchService service = new FakeWatchService();
        RecordingRegistrar registrar = RecordingRegistrar.recordingOnlyFailingOnCall(2);
        watcher = started(watcherOver(service, registrar), probe, polling);
        watcher.add(markdownUnder(tree));

        service.offer(new OverflowKey(tree));

        assertThat(probe.quietFor(settled())).isTrue();
        assertThat(registrar.registered())
                .as("a polled tree registered again would be warned of twice, or watched and polled at once")
                .hasSize(2);
        verify(polling, times(1)).polling(any(), any(), any());
    }

    @Test
    void shouldNotReportWhenPollingFindsNothingChanged() throws Exception {
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnder(tree));

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldPollIgnoringWhatTheScopeDoesNotDescendInto() throws Exception {
        Files.createDirectories(tree.resolve("backlog"));
        watcher = started(watcherWithoutInotify(), probe, polling);
        watcher.add(markdownUnderExcept(tree, tree.resolve("backlog")));

        write(tree, "backlog/ticket.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldPollOneTreeAndWatchTheOtherWhenOnlyOneCannotBeRegistered(@TempDir Path other) throws Exception {
        RecordingRegistrar registrar = RecordingRegistrar.failingOnCall(3);
        watcher = started(watcherRegisteringWith(registrar), probe, polling);
        watcher.add(markdownUnder(tree));
        watcher.add(markdownUnder(other));

        write(other, "a.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
        verify(polling, times(1)).polling(eq(other), any(), any());
    }

    @Test
    void shouldRegisterTheTreeAgainAndReportOnceOnOverflow() throws Exception {
        FakeWatchService service = new FakeWatchService();
        RecordingRegistrar registrar = RecordingRegistrar.neverFailing();
        watcher = started(watcherOver(service, registrar), probe, polling);
        watcher.add(markdownUnder(tree));
        assertThat(registrar.registered()).containsExactly(tree, tree.resolve("adr"));

        service.offer(new OverflowKey(tree));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).as("one overflow is one report").isTrue();
        assertThat(registrar.registered())
                .as("events were lost, so every directory is registered again")
                .containsExactly(tree, tree.resolve("adr"), tree, tree.resolve("adr"));
    }

    @Test
    void shouldPollWithOneWarningWhenRegistrationFailsOnTheSecondDirectory() throws Exception {
        RecordingRegistrar registrar = RecordingRegistrar.failingOnCall(2);
        watcher = started(watcherRegisteringWith(registrar), probe, polling);
        watcher.add(markdownUnder(tree));

        write(tree, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
        ArgumentCaptor<String> cause = ArgumentCaptor.forClass(String.class);
        verify(polling).polling(eq(tree), cause.capture(), any());
        assertThat(cause.getValue()).contains(tree.toString());
    }

    @Test
    void shouldCancelTheWatchesAlreadyRegisteredWhenTheWatchCannotBeSetUp() throws Exception {
        FakeWatchService service = new FakeWatchService();
        RecordingRegistrar registrar = RecordingRegistrar.recordingOnlyFailingOnCall(2);
        watcher = started(watcherOver(service, registrar), probe, polling);
        watcher.add(markdownUnder(tree));

        assertThat(registrar.cancelled())
                .as("the watches registered before the failure count against the limit polling was chosen to spare")
                .containsExactly(tree);
    }

    @Test
    void shouldFallBackToPollingWithOneWarningWhenADirectoryAddedWhileWatchingCannotBeWatched() throws Exception {
        RecordingRegistrar registrar = RecordingRegistrar.failingOnCall(3);
        watcher = started(watcherRegisteringWith(registrar), probe, polling);
        watcher.add(markdownUnder(tree));

        Files.createDirectories(tree.resolve("research"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(tree, "research/notes.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("the watch limit is hit only on the new directory, so polling must take over from the running watcher")
                .isTrue();
        ArgumentCaptor<String> cause = ArgumentCaptor.forClass(String.class);
        verify(polling).polling(eq(tree), cause.capture(), any());
        assertThat(cause.getValue()).contains(tree.toString());
    }

    @Test
    void shouldStopWatchingADirectoryThatWasDeleted() throws Exception {
        watcher = started(watcher(), probe, polling);
        watcher.add(markdownUnder(tree));

        deleteTree(tree.resolve("adr"));

        assertThat(probe.changedWithin(within())).isTrue();
    }
}
