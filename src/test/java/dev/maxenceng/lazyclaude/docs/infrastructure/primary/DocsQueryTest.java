package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocumentAt;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.docsUnavailable;
import static dev.maxenceng.lazyclaude.docs.domain.UnreadableDocumentFixture.unreadableDocument;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQueryFixture.docsQuery;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.adrFolderView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsViewOf;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.documentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unreadableDocumentView;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.adrPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondAdrPath;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;

@ExtendWith(MockitoExtension.class)
class DocsQueryTest {

    private static final DocsPayload OF_THE_PROJECT = new DocsPayload(project().root());

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

    @Test
    void shouldViewAnEmptyTreeWhenThereAreNoDocuments() {
        when(docs.paths(project())).thenReturn(List.of());

        assertThat(docsQuery(docs, watch).docsFor(OF_THE_PROJECT)).isEqualTo(emptyDocsView());
    }

    @Test
    void shouldViewFilesThenFoldersWithTheirDocuments() {
        when(docs.paths(project())).thenReturn(List.of(adrPath(), secondAdrPath(), readDocument().path(), unreadableDocument().path()));
        when(docs.read(project(), adrPath())).thenReturn(readDocumentAt(adrPath()));
        when(docs.read(project(), secondAdrPath())).thenReturn(readDocumentAt(secondAdrPath()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());
        when(docs.read(project(), unreadableDocument().path())).thenReturn(unreadableDocument());

        assertThat(docsQuery(docs, watch).docsFor(OF_THE_PROJECT))
                .isEqualTo(docsViewOf(List.of(documentView(), unreadableDocumentView()), List.of(adrFolderView())));
    }

    @Test
    void shouldViewWhyWhenTheDocsCannotBeListed() {
        when(docs.paths(project())).thenThrow(docsUnavailable());

        assertThat(docsQuery(docs, watch).docsFor(OF_THE_PROJECT)).isEqualTo(unavailableDocsView());
    }

    @Test
    void shouldHandTheListenerAViewWheneverTheDocsChange() {
        when(docs.paths(project())).thenReturn(List.of());
        List<DocsView> received = new ArrayList<>();

        docsQuery(docs, watch).subscribe(OF_THE_PROJECT, received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(eq(project()), changed.capture());
        changed.getValue().run();

        assertThat(received).containsExactly(emptyDocsView());
    }
}
