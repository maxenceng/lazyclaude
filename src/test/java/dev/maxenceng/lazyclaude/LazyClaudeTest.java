package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitFixture.linkedWorktree;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitFixture.removeBacklog;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GitFixture.repositoryWithoutBacklog;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.docsUnavailable;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQueryFixture.docsQuery;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.shell.Screen;
import dev.maxenceng.lazyclaude.shell.Shell;

@ExtendWith(MockitoExtension.class)
class LazyClaudeTest {

    private final BoardQuery realBoard = boardQuery(new ProjectLocatorRepository(), new BacklogRepository());
    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
    private final PrintStream err = new PrintStream(said, true, StandardCharsets.UTF_8);
    private final List<BoardView> opened = new ArrayList<>();
    private final List<DocsView> openedDocs = new ArrayList<>();

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

    @TempDir
    Path sandbox;

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellOutsideAGitRepository() throws Exception {
        int exit = LazyClaude.start(sandbox, realBoard, docsQuery(docs, watch), err, (view, docsView) -> opened.add(view));

        assertThat(exit).as("a missing project is a failed start").isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .as("the reason reaches the terminal")
                .isEqualTo("lazyclaude: %s is not inside a git repository%n".formatted(sandbox.toRealPath()));
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheMainWorktreeHasNoBacklog() throws Exception {
        Path repository = repositoryWithoutBacklog(sandbox.resolve("app"));

        int exit = LazyClaude.start(repository, realBoard, docsQuery(docs, watch), err, (view, docsView) -> opened.add(view));

        assertThat(exit).isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .isEqualTo("lazyclaude: the main worktree has no %s%n".formatted(repository.resolve("docs/backlog")));
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldReadTheMainWorktreesBacklogEvenWhenStartedInALinkedWorktreeThatHasOne() throws Exception {
        Path repository = repositoryWithBacklog(sandbox.resolve("app"));
        Path worktree = linkedWorktree(repository, sandbox.resolve("app.worktrees/PROJ-1-ticket"));
        removeBacklog(repository);

        int exit = LazyClaude.start(worktree, realBoard, docsQuery(docs, watch), err, (view, docsView) -> opened.add(view));

        assertThat(exit).as("the main worktree's backlog is the one that counts").isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8)).contains(repository.resolve("docs/backlog").toString());
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheBacklogCannotBeListed() throws Exception {
        Path listed = project().root().resolve("docs/backlog");
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException(listed, new IOException("denied")));

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch), err, (view, docsView) -> opened.add(view));

        assertThat(exit).isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .as("an unlistable backlog is reported, not a stack trace")
                .isEqualTo("lazyclaude: cannot list %s: denied%n".formatted(listed));
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldOpenTheShellOnTheBoardOfTheLocatedProject() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenReturn(List.of());

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch), err, (view, docsView) -> {
                    opened.add(view);
                    openedDocs.add(docsView);
                });

        assertThat(exit).isZero();
        assertThat(opened).singleElement().satisfies(view -> assertThat(view.groups()).hasSize(5));
        assertThat(openedDocs).as("the docs of the same project").containsExactly(emptyDocsView());
        assertThat(said.size()).as("a found project prints nothing").isZero();
    }

    @Test
    void shouldOpenTheShellSayingWhyWhenTheDocsCannotBeListed() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenThrow(docsUnavailable());

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch), err, (view, docsView) -> {
                    opened.add(view);
                    openedDocs.add(docsView);
                });

        assertThat(exit).as("the docs stand alone; the board still opens").isZero();
        assertThat(openedDocs).containsExactly(unavailableDocsView());
        assertThat(said.size()).as("the Docs panel says why, not the terminal").isZero();
    }

    @Test
    void shouldHandTheShellTheDocsAgainWhenTheWatcherReportsAChange() {
        when(docs.paths(project())).thenReturn(List.of());
        Shell shell = LazyClaude.shellOn(boardViewOf(), emptyDocsView(), docsQuery(docs, watch));
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(eq(project()), changed.capture());
        when(docs.paths(project())).thenReturn(List.of(readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());

        changed.getValue().run();

        assertThat(Screen.of(shell).boardPanelText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .contains(readDocument().path().fileName());
    }

    @Test
    void shouldStartWatchingWithOneWarningWhenTheWatchCannotBeSetUp() throws Exception {
        Path repository = repositoryWithBacklog(sandbox.resolve("app"));
        write(docsIn(repository), "glossary.md", markdown());
        ByteArrayOutputStream warned = new ByteArrayOutputStream();
        List<Shell> shells = new ArrayList<>();

        try (DocsWatchRepository watcher = watcherWithoutInotify(warned)) {
            DocsQuery realDocs = docsQuery(new DocsRepository(), watcher);
            int exit = LazyClaude.start(repository, realBoard, realDocs, err, (view, docsView) -> shells.add(LazyClaude.shellOn(view, docsView, realDocs)));

            assertThat(exit).isZero();
        }
        assertThat(warned.toString(StandardCharsets.UTF_8))
                .as("a watch that cannot be set up costs a warning, not the start")
                .contains("fs.inotify.max_user_watches");
        assertThat(Screen.of(shells.getFirst()).boardPanelText()).contains("glossary.md");
    }
}
