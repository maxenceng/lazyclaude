package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.deleteTree;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcher;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watching;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.pollInterval;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.settled;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeWatchFixture.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ChangeProbe;

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
    void shouldReportADocumentAddedInANewFolder() throws Exception {
        watcher = watching(watcher(), root, probe, fallback);

        write(docs, "research/tamboui.md", markdown());

        assertThat(probe.changedWithin(within())).isTrue();
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
    void shouldStartWithoutAnythingToWatchWhenTheProjectHasNoDocs() throws Exception {
        deleteTree(docs);

        watcher = watching(watcher(), root, probe, fallback);

        verifyNoInteractions(fallback);
    }

    @Test
    void shouldTellTheFallbackTheTreeAndTheSysctlWhenTheWatchCannotBeSetUp() {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        ArgumentCaptor<Reason> cause = ArgumentCaptor.forClass(Reason.class);
        verify(fallback).polling(cause.capture(), eq(pollInterval()));
        assertThat(cause.getValue().value())
                .contains(docs.toString())
                .contains("fs.inotify.max_user_watches");
    }

    @Test
    void shouldPollIgnoringTheBacklogWhenTheWatchCannotBeSetUp() throws Exception {
        watcher = watching(watcherWithoutInotify(), root, probe, fallback);

        write(docs, "backlog/ticket.md", markdown());
        assertThat(probe.quietFor(settled())).isTrue();
        write(docs, "glossary.md", "# Glossary, edited\n");

        assertThat(probe.changedWithin(within())).isTrue();
    }
}
