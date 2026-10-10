package dev.maxenceng.lazyclaude.docs.application;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsTree;
import dev.maxenceng.lazyclaude.docs.domain.DocsUnavailableException;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;

@ExtendWith(MockitoExtension.class)
class DocsApplicationServiceTest {

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

    @InjectMocks
    private DocsApplicationService service;

    @Test
    void shouldReadTheDocsTreeOfTheProject() {
        when(docs.paths(project())).thenReturn(List.of(readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());

        assertThat(service.docsFor(project())).isEqualTo(DocsTree.of(List.of(readDocument())));
    }

    @Test
    void shouldHandTheListenerAFreshTreeWheneverTheWatcherReportsAChange() {
        when(docs.paths(project())).thenReturn(List.of(readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());
        List<DocsTree> received = new ArrayList<>();

        service.subscribe(project(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(eq(project()), changed.capture());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received).containsExactly(DocsTree.of(List.of(readDocument())), DocsTree.of(List.of(readDocument())));
    }

    @Test
    void shouldKeepWatchingWhenARereadFails() {
        when(docs.paths(project()))
                .thenThrow(new DocsUnavailableException(Path.of("/work/app/docs"), new IOException("denied")))
                .thenReturn(List.of());
        List<DocsTree> received = new ArrayList<>();

        service.subscribe(project(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(eq(project()), changed.capture());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received)
                .as("a failed read leaves the previous tree on screen, and the next change is read again")
                .containsExactly(DocsTree.of(List.of()));
    }
}
