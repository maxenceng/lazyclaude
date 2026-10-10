package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.deleteTree;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.settled;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcher;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherOver;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherRegisteringWith;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watching;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.within;
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

class DocsWatchRepositoryTest {

    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
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
        watcher = watching(watcher(said), root, probe);

        write(docs, "glossary.md", "# Glossary\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportAnAddedDocument() throws Exception {
        watcher = watching(watcher(said), root, probe);

        write(docs, "new.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportADeletedDocument() throws Exception {
        watcher = watching(watcher(said), root, probe);

        Files.delete(docs.resolve("adr/0001-record.md"));

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldReportOnceForABurstOfSaves() throws Exception {
        watcher = watching(watcher(said), root, probe);

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
        watcher = watching(watcher(said), root, probe);

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
        watcher = watching(watcher(said), root, probe);

        Files.createDirectories(docs.resolve("research/old"));
        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "research/old/a.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldIgnoreChangesUnderTheBacklog() throws Exception {
        watcher = watching(watcher(said), root, probe);

        write(docs, "backlog/DOCS-1.md", markdown());
        write(docs, "backlog/nested/DOCS-2.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldIgnoreAChangeToAFileThatIsNotMarkdown() throws Exception {
        watcher = watching(watcher(said), root, probe);

        write(docs, "adr/.0001-record.md.swp", "x");

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldSayNothingWhenTheWatchIsSetUp() {
        watcher = watching(watcher(said), root, probe);

        assertThat(said.size()).isZero();
    }

    @Test
    void shouldStartWithoutAnythingToWatchWhenTheProjectHasNoDocs() throws Exception {
        deleteTree(docs);

        watcher = watching(watcher(said), root, probe);

        assertThat(said.size()).as("no docs is not a failed watch").isZero();
    }

    @Test
    void shouldWarnOnceNamingTheTreeAndTheSysctlWhenTheWatchCannotBeSetUp() {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        assertThat(said.toString(StandardCharsets.UTF_8))
                .contains(docs.toString())
                .contains("fs.inotify.max_user_watches")
                .contains("User limit of inotify watches reached")
                .hasLineCount(1);
    }

    @Test
    void shouldPollForChangesWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        write(docs, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldPollForAnAddedDocumentInANewFolder() throws Exception {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        write(docs, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
    }

    @Test
    void shouldNotWarnAgainWhileItKeepsPolling() throws Exception {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        write(docs, "one.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();
        write(docs, "two.md", markdown());
        assertThat(probe.changedWithin(within())).isTrue();

        assertThat(said.toString(StandardCharsets.UTF_8)).hasLineCount(1);
    }

    @Test
    void shouldNotReportWhenPollingFindsNothingChanged() throws Exception {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldPollIgnoringTheBacklog() throws Exception {
        watcher = watching(watcherWithoutInotify(said), root, probe);

        write(docs, "backlog/DOCS-1.md", markdown());

        assertThat(probe.quietFor(settled())).isTrue();
    }

    @Test
    void shouldRegisterTheTreeAgainAndReportOnceOnOverflow() throws Exception {
        FakeWatchService service = new FakeWatchService();
        RecordingRegistrar registrar = RecordingRegistrar.neverFailing();
        watcher = watching(watcherOver(service, registrar, said), root, probe);
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
        watcher = watching(watcherRegisteringWith(registrar, said), root, probe);

        write(docs, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .contains(docs.toString())
                .contains("fs.inotify.max_user_watches")
                .hasLineCount(1);
    }
}
