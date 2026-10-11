package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.docsUnavailable;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.unavailableDocs;
import static dev.maxenceng.lazyclaude.docs.domain.UnreadableDocumentFixture.unreadableDocument;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class DocsManagerTest {

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private DocsManager manager;

    @Test
    void shouldBuildTheTreeFromEveryDocumentTheProjectHolds() {
        when(docs.paths(project())).thenReturn(List.of(readDocument().path(), unreadableDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());
        when(docs.read(project(), unreadableDocument().path())).thenReturn(unreadableDocument());

        assertThat(manager.docsFor(project()))
                .isEqualTo(DocsTree.of(List.of(readDocument(), unreadableDocument())));
    }

    @Test
    void shouldNotReadWhatTheTreeWouldLeaveOut() {
        DocumentPath backlog = new DocumentPath("backlog/ticket.md");
        DocumentPath image = new DocumentPath("adr/diagram.png");
        when(docs.paths(project())).thenReturn(List.of(backlog, image, readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());

        manager.docsFor(project());

        verify(docs, never()).read(project(), backlog);
        verify(docs, never()).read(project(), image);
    }

    @Test
    void shouldBeAnEmptyTreeWhenTheProjectHoldsOnlyTheBacklog() {
        when(docs.paths(project())).thenReturn(List.of(new DocumentPath("backlog/ticket.md")));

        assertThat(manager.docsFor(project())).isEqualTo(DocsTree.of(List.of()));
    }

    @Test
    void shouldSayWhyWhenTheDocsCannotBeListed() {
        when(docs.paths(project())).thenThrow(docsUnavailable());

        assertThat(manager.docsFor(project()))
                .as("docs that cannot be listed are shown as such, not a failed start")
                .isEqualTo(unavailableDocs());
    }

    @Test
    void shouldAskTheWatcherToTellItWhenTheDocsChange() {
        Runnable changed = () -> {};

        manager.watch(project(), changed);

        verify(watch).watch(eq(project()), eq(changed), any());
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheWatchFallsBackToPolling() {
        manager.watch(project(), () -> {});
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(eq(project()), any(), fallback.capture());

        fallback.getValue().polling("cannot watch /work/app/docs (User limit of inotify watches reached)", Duration.ofSeconds(5));

        verify(warnings)
                .raise(new Warning(
                        "docs: cannot watch /work/app/docs (User limit of inotify watches reached); checking every 5 s instead. If the cause is the watch limit, raise fs.inotify.max_user_watches."));
    }

    @Test
    void shouldRaiseNothingWhileTheWatchHoldsUp() {
        manager.watch(project(), () -> {});

        verifyNoInteractions(warnings);
    }
}
