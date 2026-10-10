package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.UnreadableDocumentFixture.unreadableDocument;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

@ExtendWith(MockitoExtension.class)
class DocsManagerTest {

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

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
        DocumentPath backlog = new DocumentPath("backlog/DOCS-1.md");
        DocumentPath image = new DocumentPath("adr/diagram.png");
        when(docs.paths(project())).thenReturn(List.of(backlog, image, readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());

        manager.docsFor(project());

        verify(docs, never()).read(project(), backlog);
        verify(docs, never()).read(project(), image);
    }

    @Test
    void shouldBeAnEmptyTreeWhenTheProjectHoldsOnlyTheBacklog() {
        when(docs.paths(project())).thenReturn(List.of(new DocumentPath("backlog/DOCS-1.md")));

        assertThat(manager.docsFor(project()).root().documents()).isEmpty();
        assertThat(manager.docsFor(project()).root().folders()).isEmpty();
    }

    @Test
    void shouldLetTheListingsFailureThrough() {
        when(docs.paths(project())).thenThrow(new DocsUnavailableException(Path.of("/work/app/docs"), new IOException("denied")));

        assertThatThrownBy(() -> manager.docsFor(project())).isExactlyInstanceOf(DocsUnavailableException.class);
    }

    @Test
    void shouldAskTheWatcherToTellItWhenTheDocsChange() {
        Runnable changed = () -> {};

        manager.watch(project(), changed);

        verify(watch).watch(project(), changed);
    }
}
