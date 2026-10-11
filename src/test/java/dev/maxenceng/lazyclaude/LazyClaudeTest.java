package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.docsUnavailable;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQueryFixture.docsQuery;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.unavailableSessionsView;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommandFixture.warningCommand;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQueryFixture.warningQuery;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.removeBacklog;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithBacklog;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithoutBacklog;
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
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogRepository;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;
import dev.maxenceng.lazyclaude.shell.Screen;
import dev.maxenceng.lazyclaude.shell.Shell;

@ExtendWith(MockitoExtension.class)
class LazyClaudeTest {

    private final BoardQuery realBoard = boardQuery(new ProjectLocatorRepository(), new BacklogRepository());
    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
    private final PrintStream err = new PrintStream(said, true, StandardCharsets.UTF_8);
    private final WarningRepository raised = new WarningRepository();
    private final List<BoardView> opened = new ArrayList<>();
    private final List<DocsView> openedDocs = new ArrayList<>();
    private final List<SessionsView> openedSessions = new ArrayList<>();

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private DocsPort docs;

    @Mock
    private DocsWatchPort watch;

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private SessionWatchPort sessionWatch;

    @TempDir
    Path sandbox;

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellOutsideAGitRepository() throws Exception {
        int exit = LazyClaude.start(sandbox, realBoard, docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> opened.add(view));

        assertThat(exit).as("a missing project is a failed start").isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .as("the reason reaches the terminal")
                .isEqualTo("lazyclaude: %s is not inside a git repository%n".formatted(sandbox.toRealPath()));
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheMainWorktreeHasNoBacklog() throws Exception {
        Path repository = repositoryWithoutBacklog(sandbox.resolve("app"));

        int exit = LazyClaude.start(repository, realBoard, docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> opened.add(view));

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

        int exit = LazyClaude.start(worktree, realBoard, docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> opened.add(view));

        assertThat(exit).as("the main worktree's backlog is the one that counts").isNotZero();
        assertThat(said.toString(StandardCharsets.UTF_8)).contains(repository.resolve("docs/backlog").toString());
        assertThat(opened).isEmpty();
    }

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellWhenTheBacklogCannotBeListed() throws Exception {
        Path listed = project().root().resolve("docs/backlog");
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException(listed, new IOException("denied")));

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> opened.add(view));

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

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> {
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

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> {
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
        Shell shell = LazyClaude.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), warningQuery(raised));
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(eq(project()), changed.capture(), any());
        when(docs.paths(project())).thenReturn(List.of(readDocument().path()));
        when(docs.read(project(), readDocument().path())).thenReturn(readDocument());

        changed.getValue().run();

        assertThat(Screen.of(shell).boardPanelText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .contains(readDocument().path().fileName());
    }

    @Test
    void shouldStartWatchingAndShowTheWarningOnTheFirstFrameWhenTheWatchCannotBeSetUp() throws Exception {
        Path repository = repositoryWithBacklog(sandbox.resolve("app"));
        write(docsIn(repository), "glossary.md", markdown());
        List<Shell> shells = new ArrayList<>();

        try (DocsWatchRepository watcher = watcherWithoutInotify()) {
            DocsQuery realDocs = docsQuery(new DocsRepository(), watcher, raised);
            int exit = LazyClaude.start(repository, realBoard, realDocs, sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> shells.add(LazyClaude.shellOn(view, docsView, sessionsView, realDocs, sessionQuery(sessions, locator, sessionWatch, raised), warningQuery(raised))));

            assertThat(exit).isZero();
        }
        Screen first = Screen.of(shells.getFirst());
        assertThat(first.statusLine())
                .as("a watch that cannot be set up costs a warning on the screen, raised before the screen existed")
                .contains("⚠ docs: cannot watch");
        assertThat(first.boardPanelText()).as("and not the start").contains("glossary.md");
        assertThat(said.size()).as("nothing reaches stderr while the screen is up").isZero();
    }

    @Test
    void shouldShowAWarningRaisedAfterTheShellWasComposed() {
        Shell shell = LazyClaude.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), warningQuery(raised));
        assertThat(Screen.of(shell).statusLine()).isBlank();

        raised.raise(warning());

        assertThat(Screen.of(shell).statusLine()).endsWith(warning().text());
        assertThat(said.size()).as("nothing reaches stderr while the screen is up").isZero();
    }

    @Test
    void shouldShowAWarningWhenAThreadOtherThanMainStopsOnAFailureNobodyCaught() throws Exception {
        Shell shell = LazyClaude.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), warningQuery(raised));
        Thread.UncaughtExceptionHandler handler = LazyClaude.warnOfStoppedThreads(Thread.currentThread(), warningCommand(raised), err);

        Thread.ofPlatform()
                .name("docs-watch")
                .uncaughtExceptionHandler(handler)
                .start(() -> {
                    throw new IllegalStateException("gone");
                })
                .join();

        assertThat(Screen.of(shell).statusLine()).contains("⚠ docs-watch stopped (java.lang.IllegalStateException: gone)");
        assertThat(said.size()).as("a dying watcher must not print its stack trace over the screen").isZero();
    }

    @Test
    void shouldPrintAFailureThatEndsTheMainThreadSinceTheScreenIsGoneByThen() {
        Thread.UncaughtExceptionHandler handler = LazyClaude.warnOfStoppedThreads(Thread.currentThread(), warningCommand(raised), err);

        handler.uncaughtException(Thread.currentThread(), new IllegalStateException("gone"));

        assertThat(said.toString(StandardCharsets.UTF_8))
                .as("main carries the shell, so a failure there would otherwise vanish into a warning nobody sees")
                .contains("java.lang.IllegalStateException: gone");
        List<WarningView> shown = new ArrayList<>();
        warningQuery(raised).subscribe(shown::add);
        assertThat(shown).isEmpty();
    }

    @Test
    void shouldOpenTheShellOnTheLiveSessionsOfTheSameProject() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenReturn(List.of());
        when(sessions.liveSessions(any())).thenReturn(List.of());

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> openedSessions.add(sessionsView));

        assertThat(exit).isZero();
        assertThat(openedSessions).containsExactly(emptySessionsView());
    }

    @Test
    void shouldOpenTheShellSayingWhyWhenNoSourceOfSessionsAnswers() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenReturn(List.of());
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        int exit = LazyClaude.start(project().root(), boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), err, (view, docsView, sessionsView) -> openedSessions.add(sessionsView));

        assertThat(exit).as("the sessions stand alone; the board still opens").isZero();
        assertThat(openedSessions).containsExactly(unavailableSessionsView());
        assertThat(said.size()).as("the Sessions panel says why, not the terminal").isZero();
    }

    @Test
    void shouldHandTheShellTheSessionsAgainWhenTheWatcherReportsAChange() {
        Shell shell = LazyClaude.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), docsQuery(docs, watch, raised), sessionQuery(sessions, locator, sessionWatch, raised), warningQuery(raised));
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(sessionWatch).watch(changed.capture(), any());
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        changed.getValue().run();

        assertThat(Screen.of(shell).dimText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .anySatisfy(line -> assertThat(line).contains("sessions unavailable"));
    }
}
