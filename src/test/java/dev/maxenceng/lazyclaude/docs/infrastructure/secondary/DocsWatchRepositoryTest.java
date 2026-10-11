package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.deleteTree;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.pollInterval;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.settled;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcher;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherOver;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherRegisteringWith;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watching;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.within;
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

import dev.maxenceng.lazyclaude.docs.domain.WatchFallback;
import dev.maxenceng.lazyclaude.docs.published.Reason;

@ExtendWith(MockitoExtension.class)
class DocsWatchRepositoryTest {

    @Mock
    private WatchFallback fallback;


    private final ChangeProbe probe = new ChangeProbe();
    private DocsWatchRepository watcher;

    @TempDir
    Path root;

    private Path docs;

    @BeforeEach
    void createDocs() throws IOException {
        docs = docsIn(root);
        write(docs, "glossary.md", markdown());
        Files.createDirectories(docs.resolve("backlog"));
        write(docs, "adr/0001-record.md", markdown());
    }

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldReportASavedDocument() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        write(docs, "glossary.md", "# Glossary\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportAnAddedDocument() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        write(docs, "new.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADeletedDocument() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.delete(docs.resolve("adr/0001-record.md"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportAFolderMovedOutOfTheTree() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.move(docs.resolve("adr"), root.resolve("adr"));

        assertThat(probe.changedWithin(within()))
                .as("a folder moved away fires one delete for the folder and none for the documents in it")
                .isTrue();
    }

    @Test
    void shouldReportOnceForABurstOfSaves() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        for (int save = 0; save < 5; save++) {
            write(docs, "burst-%d.md".formatted(save), markdown());
        }

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled()))
                .as("one editor save fires several events, and the tree is read once for them")
                .isTrue();
    }

    @Test
    void shouldWatchAFolderAddedAfterItStarted() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.createDirectories(docs.resolve("research"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("a document saved in the new folder shows up without a restart")
                .isTrue();
    }

    @Test
    void shouldWatchAFolderNestedInAnAddedFolder() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.createDirectories(docs.resolve("research/old"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "research/old/a.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldWatchAFolderAddedInsideARenamedFolder() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        Files.move(docs.resolve("adr"), docs.resolve("decisions"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        Files.createDirectories(docs.resolve("decisions/drafts"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "decisions/drafts/a.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("the renamed folder's watch still names its old path, so the folder added in it was never watched")
                .isTrue();
    }

    @Test
    void shouldIgnoreChangesUnderTheBacklog() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        write(docs, "backlog/ticket.md", markdown());
        write(docs, "backlog/nested/DOCS-2.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldIgnoreAChangeToAFileThatIsNotMarkdown() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        write(docs, "adr/.0001-record.md.swp", "x");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldSayNothingWhenTheWatchIsSetUp() {
        watcher = watching(watcher(), root, probe, fallback);

        verifyNoInteractions(fallback);
    }

    @Test
    void shouldStartWithoutAnythingToWatchWhenTheProjectHasNoDocs() throws Exception {
        deleteTree(docs);

        watcher = watching(watcher(), root, probe, fallback);

        verifyNoInteractions(fallback);
    }

    @Test
    void shouldWarnOnceNamingTheTreeAndTheSysctlWhenTheWatchCannotBeSetUp() {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).polling(cause.capture(), eq(pollInterval()));
        assertThat(cause.getValue().value()).contains(docs.toString())
                .contains("User limit of inotify watches reached")
                .contains("fs.inotify.max_user_watches");
    }

    @Test
    void shouldPollForChangesWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        write(docs, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldPollForAnAddedDocumentInANewFolder() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        write(docs, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldNotWarnAgainWhileItKeepsPolling() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        write(docs, "one.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();
        write(docs, "two.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();

        verify(fallback, times(1)).polling(any(), any());
    }

    @Test
    void shouldNotReportWhenPollingFindsNothingChanged() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldPollIgnoringTheBacklog() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        write(docs, "backlog/ticket.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldRegisterTheTreeAgainAndReportOnceOnOverflow() throws Exception {
        FakeWatchService service = new FakeWatchService();
        RecordingRegistrar registrar = RecordingRegistrar.neverFailing();
        watcher = watching(watcherOver(service, registrar), root, probe, fallback);
        assertThat(registrar.registered()).containsExactly(docs, docs.resolve("adr"));

        service.offer(new OverflowKey(docs));

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).as("one overflow is one report").isTrue();
        assertThat(registrar.registered())
                .as("events were lost, so every directory is registered again")
                .containsExactly(docs, docs.resolve("adr"), docs, docs.resolve("adr"));
    }

    @Test
    void shouldPollWithOneWarningWhenRegistrationFailsOnTheSecondDirectory() throws Exception {
        RecordingRegistrar registrar = RecordingRegistrar.failingOnCall(2);
        watcher = watching(watcherRegisteringWith(registrar), root, probe, fallback);

        write(docs, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).polling(cause.capture(), any());
        assertThat(cause.getValue().value()).contains(docs.toString());
    }

    @Test
    void shouldLetGoOfTheWatchWhenItCannotBeSetUp() throws Exception {
        FakeWatchService service = new FakeWatchService();
        watcher = watching(watcherOver(service, RecordingRegistrar.failingOnCall(1)), root, probe, fallback);

        assertThat(service.isClosed())
                .as("the watches registered before the failure count against the limit polling was chosen to spare")
                .isTrue();
    }

    @Test
    void shouldFallBackToPollingWithOneWarningWhenAFolderAddedWhileWatchingCannotBeWatched() throws Exception {
        RecordingRegistrar registrar = RecordingRegistrar.failingOnCall(3);
        watcher = watching(watcherRegisteringWith(registrar), root, probe, fallback);

        Files.createDirectories(docs.resolve("research"));
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "research/notes.md", markdown());

        assertThat(probe.changedWithin(within()))
                .as("the watch limit is hit only on the new folder, so polling must take over from the running watcher")
                .isTrue();
        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).polling(cause.capture(), any());
        assertThat(cause.getValue().value()).contains(docs.toString());
    }
}
