package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.ContextsFixture.contexts;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.unreadChanges;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.secondTicketId;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.UnavailableDocsFixture.docsUnavailable;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQueryFixture.docsQuery;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchFixture.watcherWithoutInotify;
import static dev.maxenceng.lazyclaude.review.domain.PullRequestFixture.failingPullRequest;
import static dev.maxenceng.lazyclaude.review.domain.ReviewManagerFixture.pullRequestsUnavailable;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQueryFixture.reviewQuery;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLogPort;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionId;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFacts;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.transcriptsRead;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketIds;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommandFixture.warningCommand;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQueryFixture.warningQuery;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.linkedWorktree;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithACommit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.GitFixture.repositoryWithoutACommit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.BoardWatchPort;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardReadingView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.UnavailableBoardView;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsWatchPort;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

@ExtendWith(MockitoExtension.class)
class ShellStartupTest {

    private final List<Throwable> printed = new ArrayList<>();
    private final WarningRepository raised = new WarningRepository();
    private final List<BoardReadingView> opened = new ArrayList<>();
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
    private PastSessionPort pastSessions;

    @Mock
    private OpenPullRequestPort pullRequests;

    @Mock
    private MergedPullRequestPort mergedPullRequests;

    @Mock
    private PullRequestWatchPort pullRequestWatch;

    @Mock
    private SessionWatchPort sessionWatch;

    @Mock
    private BoardWatchPort boardWatch;

    @TempDir
    Path sandbox;

    @Test
    void shouldSayWhyAndExitNonZeroWithoutOpeningTheShellOutsideAGitRepository() throws Exception {
        Optional<Reason> refused = ShellStartup.start(sandbox, contexts(boardQuery(new ProjectLocatorRepository(), backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> opened.add(view));

        assertThat(refused)
                .as("a missing project is a failed start, and its reason is what main prints")
                .map(Reason::value)
                .contains("%s is not inside a git repository".formatted(sandbox.toRealPath()));
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
    }

    @Test
    void shouldOpenTheShellSayingWhyWhenTheProjectHasNoGitHubRemote() throws Exception {
        Path repository = repositoryWithoutACommit(sandbox.resolve("app"));
        when(backlog.entries(any())).thenThrow(new BacklogUnavailableException("this project has no GitHub remote, so it has no backlog"));

        Optional<Reason> refused = ShellStartup.start(repository, contexts(boardQuery(new ProjectLocatorRepository(), backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> opened.add(view));

        assertThat(refused).as("the Board says why; the docs and sessions still open").isEmpty();
        assertThat(opened).singleElement().satisfies(view -> assertThat(view).isInstanceOfSatisfying(UnavailableBoardView.class, unavailable -> assertThat(unavailable.reason()).isEqualTo(new Reason("this project has no GitHub remote, so it has no backlog"))));
    }

    @Test
    void shouldReadTheIssuesOfTheMainWorktreesProjectWhenStartedInALinkedWorktree() throws Exception {
        Path repository = repositoryWithACommit(sandbox.resolve("app"));
        Path worktree = linkedWorktree(repository, sandbox.resolve("app.worktrees/PROJ-1-ticket"));
        when(backlog.entries(any())).thenReturn(List.of());

        ShellStartup.start(worktree, contexts(boardQuery(new ProjectLocatorRepository(), backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> opened.add(view));

        verify(backlog).entries(new Project(repository));
        assertThat(opened).singleElement().satisfies(view -> assertThat(view.root().value()).isEqualTo(repository));
    }

    @Test
    void shouldOpenTheShellSayingWhyWhenGitHubCannotBeReached() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException("gh is not logged in; run gh auth login"));
        when(docs.paths(project())).thenReturn(List.of());

        Optional<Reason> refused = ShellStartup.start(project().root(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> opened.add(view));

        assertThat(refused).isEmpty();
        assertThat(opened).singleElement().satisfies(view -> assertThat(view).isInstanceOfSatisfying(UnavailableBoardView.class, unavailable -> assertThat(unavailable.reason()).isEqualTo(new Reason("gh is not logged in; run gh auth login"))));
    }

    @Test
    void shouldOpenTheShellOnTheBoardOfTheLocatedProject() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenReturn(List.of());

        Optional<Reason> refused = ShellStartup.start(project().root(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> {
                    opened.add(view);
                    openedDocs.add(docsView);
                });

        assertThat(refused).isEmpty();
        assertThat(opened).singleElement().satisfies(view -> assertThat(view).isInstanceOfSatisfying(BoardView.class, read -> assertThat(read.groups()).hasSize(5)));
        assertThat(openedDocs).as("the docs of the same project").containsExactly(emptyDocsView());
    }

    @Test
    void shouldOpenTheShellSayingWhyWhenTheDocsCannotBeListed() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenThrow(docsUnavailable());

        Optional<Reason> refused = ShellStartup.start(project().root(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> {
                    opened.add(view);
                    openedDocs.add(docsView);
                });

        assertThat(refused).as("the docs stand alone; the board still opens").isEmpty();
        assertThat(openedDocs).containsExactly(unavailableDocsView());
    }

    @Test
    void shouldHandTheShellTheBoardAgainWhenTheBoardWatcherReportsAChange() {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog, unreadChanges(), boardWatch, raised), docsQuery(docs, watch, raised), sessionQuery(sessions, quietPastSessions(), emptyLaunchLogPort(), sessionWatch, mock(TranscriptWatchPort.class), raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(boardWatch).watch(eq(project()), changed.capture());
        when(backlog.entries(project())).thenReturn(List.of(ticket()));

        changed.getValue().run();

        assertThat(Screen.of(shell).boardPanelText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .contains("#1");
    }

    @Test
    void shouldClearTheWarningOfAFailedBoardReadOnTheStatusLineWhenTheNextReadIsGood() {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenThrow(new BacklogUnavailableException("gh is not logged in; run gh auth login")).thenReturn(List.of());
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog, unreadChanges(), boardWatch, raised), docsQuery(docs, watch, raised), sessionQuery(sessions, quietPastSessions(), emptyLaunchLogPort(), sessionWatch, mock(TranscriptWatchPort.class), raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(boardWatch).watch(eq(project()), changed.capture());
        changed.getValue().run();
        assertThat(Screen.of(shell).statusLine()).contains("⚠ board: gh is not logged in; run gh auth login");

        changed.getValue().run();

        assertThat(Screen.of(shell).statusLine()).as("the next good read withdraws it").isBlank();
    }

    @Test
    void shouldHandTheShellTheDocsAgainWhenTheWatcherReportsAChange() {
        when(docs.paths(project())).thenReturn(List.of());
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
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
        Path repository = repositoryWithACommit(sandbox.resolve("app"));
        when(backlog.entries(any())).thenReturn(List.of());
        write(docsIn(repository), "glossary.md", markdown());
        List<Shell> shells = new ArrayList<>();

        try (DocsWatchRepository watcher = watcherWithoutInotify()) {
            DocsQuery realDocs = docsQuery(new DocsRepository(), watcher, raised);
            Optional<Reason> refused = ShellStartup.start(repository, contexts(boardQuery(new ProjectLocatorRepository(), backlog), realDocs, sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> shells.add(ShellStartup.shellOn(view, docsView, sessionsView, contexts(boardQuery(new ProjectLocatorRepository(), backlog), realDocs, sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true)));

            assertThat(refused).isEmpty();
        }
        Screen first = Screen.of(shells.getFirst());
        assertThat(first.statusLine())
                .as("a watch that cannot be set up costs a warning on the screen, raised before the screen existed")
                .contains("⚠ docs: cannot watch");
        assertThat(first.boardPanelText()).as("and not the start").contains("glossary.md");
    }

    @Test
    void shouldShowAWarningRaisedAfterTheShellWasComposed() {
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        assertThat(Screen.of(shell).statusLine()).isBlank();

        raised.raise(warning());

        assertThat(Screen.of(shell).statusLine()).endsWith(warning().text());
    }

    @Test
    void shouldShowAWarningWhenAThreadOtherThanMainStopsOnAFailureNobodyCaught() throws Exception {
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        Thread.UncaughtExceptionHandler handler = ShellStartup.warnOfStoppedThreads(Thread.currentThread(), warningCommand(raised), printed::add);

        Thread.ofPlatform()
                .name("docs-watch")
                .uncaughtExceptionHandler(handler)
                .start(() -> {
                    throw new IllegalStateException("gone");
                })
                .join();

        assertThat(Screen.of(shell).statusLine()).contains("⚠ docs-watch stopped (java.lang.IllegalStateException: gone)");
        assertThat(printed).as("a dying watcher must not print its stack trace over the screen").isEmpty();
    }

    @Test
    void shouldPrintAFailureThatEndsTheMainThreadSinceTheScreenIsGoneByThen() {
        Thread.UncaughtExceptionHandler handler = ShellStartup.warnOfStoppedThreads(Thread.currentThread(), warningCommand(raised), printed::add);

        handler.uncaughtException(Thread.currentThread(), new IllegalStateException("gone"));

        assertThat(printed)
                .as("main carries the shell, so a failure there would otherwise vanish into a warning nobody sees")
                .singleElement()
                .hasToString("java.lang.IllegalStateException: gone");
        List<WarningView> shown = new ArrayList<>();
        warningQuery(raised).subscribe(shown::add);
        assertThat(shown).isEmpty();
    }

    @Test
    void shouldHandTheSessionsTheRootAndTheIdOfEveryTicketOnTheBoard() {
        BoardView board = boardViewOf(ticketView(), secondTicketView(), doneTicketView());

        assertThat(ShellStartup.sessionPayload(board))
                .isEqualTo(new SessionPayload(project().root(), linkedTicketIds(ticketId().value(), secondTicketId().value(), ticketId().value())));
    }

    @Test
    void shouldOpenTheShellOnTheSessionsOfTheSameProject() throws Exception {
        when(locator.locate(any())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());
        when(docs.paths(project())).thenReturn(List.of());

        Optional<Reason> refused = ShellStartup.start(project().root(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised)), (view, docsView, sessionsView) -> openedSessions.add(sessionsView));

        assertThat(refused).isEmpty();
        assertThat(openedSessions).containsExactly(emptySessionsView());
    }

    @Test
    void shouldHandTheShellTheSessionsAgainWhenTheWatcherReportsAChange() {
        LaunchLogPort launchLog = mock(LaunchLogPort.class);
        when(launchLog.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));
        Shell shell = ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, pastSessions, launchLog, sessionWatch, mock(TranscriptWatchPort.class), raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(sessionWatch).watch(any(), changed.capture(), any());
        when(pastSessions.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));

        changed.getValue().run();

        assertThat(Screen.of(shell).dimText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .anySatisfy(line -> assertThat(line).contains("unlinked"));
    }

    @Test
    void shouldHandTheShellTheOpenPullRequestsWhenTheWatchRuns() {
        Shell shell = ShellStartup.shellOn(boardViewOf(slugTicketView()), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        ArgumentCaptor<Runnable> refresh = ArgumentCaptor.forClass(Runnable.class);
        verify(pullRequestWatch).watch(refresh.capture());
        when(pullRequests.openPullRequests(project())).thenReturn(List.of(failingPullRequest()));

        refresh.getValue().run();

        assertThat(Screen.of(shell).boardPanelText())
                .as("watcher, application and shell are joined for the project the board was read from")
                .contains("✗");
    }

    @Test
    void shouldRaiseNoWarningWhenGitHubCannotBeRead() {
        Shell shell = ShellStartup.shellOn(boardViewOf(slugTicketView()), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised)), ReservedChords.defaults(), true);
        ArgumentCaptor<Runnable> refresh = ArgumentCaptor.forClass(Runnable.class);
        verify(pullRequestWatch).watch(refresh.capture());
        when(pullRequests.openPullRequests(project())).thenThrow(pullRequestsUnavailable());

        refresh.getValue().run();

        assertThat(Screen.of(shell).statusLine()).as("the detail says why, the status line stays quiet").isBlank();
    }

    @Test
    void shouldEndEveryHostedSessionWhenTheShellReturns() throws Exception {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);

        ShellStartup.runThenEndHosted(() -> {}, hosting);

        verify(hosting).endAll();
    }

    @Test
    void shouldEndEveryHostedSessionWhenTheShellFails() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);

        assertThatThrownBy(() -> ShellStartup.runThenEndHosted(
                        () -> {
                            throw new IllegalStateException("render failed");
                        },
                        hosting))
                .hasMessage("render failed");

        verify(hosting).endAll();
    }

    @Test
    void shouldHaveTheShellRepaintWhenAHostedSessionDraws() {
        HostedEmulatorQuery emulators = mock(HostedEmulatorQuery.class);

        ShellStartup.shellOn(boardViewOf(), emptyDocsView(), emptySessionsView(), contexts(boardQuery(locator, backlog), docsQuery(docs, watch, raised), sessionQuery(sessions, emptyLaunchLogPort(), sessionWatch, raised), reviewQuery(pullRequests, mergedPullRequests, pullRequestWatch), warningQuery(raised), emulators), ReservedChords.defaults(), true);

        verify(emulators).onScreenChange(any());
    }
}
