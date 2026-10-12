package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openLive;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.onBranches;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ranOnBranch;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.hostedLiveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.oldestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedIn;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedInProject;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.notLocated;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionActiveMinutesAgo;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionIn;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.newestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.oldestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedIn;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWaitingFor;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWith;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.sdkCliSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.sessionWithoutEntrypoint;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.advancingClock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.noHostedSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionManager;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.cacheUnwritable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnreadable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnlistable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.otherProjectDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.Hosting;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.ProjectUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private PastSessionPort past;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private WarningPort warnings;

    @Mock
    private Consumer<Sessions> listener;

    @Test
    void shouldListASessionWhoseDirectoryBelongsToTheProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldMeasureEveryAgeOfOneReadFromTheSameInstant() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        AvailableSessions read = (AvailableSessions) new SessionManager(sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings, new SessionClock(advancingClock()), noHostedSessions()).sessionsFor(project(), noTickets());

        assertThat(read.ageOf(liveSession()))
                .as("rows of one read drawn at different instants would disagree on how long ago they started")
                .isEqualTo(read.ageOf(liveSession()));
    }

    @Test
    void shouldListASessionInATicketWorktreeBecauseTheLocatorPointsItAtTheMainWorktree() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(worktreeDirectory())));
        when(locator.locate(worktreeDirectory())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(sessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListASessionOfAnotherProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(otherProjectDirectory())));
        when(locator.locate(otherProjectDirectory())).thenReturn(new Project(Path.of("/work/other")));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldNotListASessionWhoseDirectoryBelongsToNoProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(outsideGitDirectory()), reportedSession()));
        when(locator.locate(outsideGitDirectory())).thenThrow(new ProjectNotFoundException("not inside a git repository"));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldSayWhyWhenTheProjectOfASessionCannotBeLocated() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession())))
                .thenThrow(new ProjectUnavailableException(reason().value(), new InterruptedException()));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("a git that failed says nothing about where the session is, so the panel must not claim there is none")
                .isEqualTo(unavailableSessions());
    }

    @Test
    void shouldLocateEachDistinctDirectoryOnce() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession(), newestReportedSession(), oldestReportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets());

        verify(locator, times(1)).locate(workingDirectoryOf(reportedSession()));
    }

    @Test
    void shouldNotListAnSdkCliSession() {
        when(sessions.liveSessions(any())).thenReturn(List.of(sdkCliSession(), reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListASessionThatReportsNoEntrypoint() {
        when(sessions.liveSessions(any())).thenReturn(List.of(sessionWithoutEntrypoint()));
        when(locator.locate(workingDirectoryOf(sessionWithoutEntrypoint()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("claude agents --json has no entrypoint, so an sdk-cli session cannot be told apart there")
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldOrderNewestStartFirst() {
        when(sessions.liveSessions(any())).thenReturn(List.of(oldestReportedSession(), newestReportedSession(), reportedSession()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), liveSession(), oldestSession())));
    }

    @Test
    void shouldKeepTheOrderWhenAStateChanges() {
        when(locator.locate(any())).thenReturn(project());
        when(sessions.liveSessions(any()))
                .thenReturn(List.of(reportedWith(ReportedStatus.BUSY), newestReportedSession()))
                .thenReturn(List.of(reportedWith(ReportedStatus.IDLE), newestReportedSession()))
                .thenReturn(List.of(reportedWaitingFor(inputNeeded()), newestReportedSession()));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new Busy()))));
        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new YourTurn()))));
        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new WaitingForInput(inputNeeded())))));
    }

    @Test
    void shouldBeBusyWhenTheStatusIsShell() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWith(ReportedStatus.SHELL)));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Busy()))));
    }

    @Test
    void shouldWaitForPermissionOnAPermissionPrompt() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWaitingFor(permissionPrompt())));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new WaitingForPermission()))));
    }

    @Test
    void shouldWaitForInputWhenNoReasonIsGiven() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWaitingFor(new NoWaitingReason())));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new WaitingForInput(new NoWaitingReason())))));
    }

    @Test
    void shouldIgnoreAReasonOnASessionThatIsNotWaiting() {
        when(sessions.liveSessions(any()))
                .thenReturn(List.of(reportedSessionBuilder()
                        .status(ReportedStatus.IDLE)
                        .waitingFor(permissionPrompt())
                        .build()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("only a waiting session is blocked, whatever stale reason its record carries")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new YourTurn()))));
    }

    @Test
    void shouldListASessionWhoseStatusItDoesNotKnowAsUnknown() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWith(ReportedStatus.UNKNOWN)));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("a newer Claude Code's status must not hide a session that runs")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Unknown()))));
    }

    @Test
    void shouldListTheProjectsSessionsWhenASessionElsewhereReportsAStatusItDoesNotKnow() {
        when(sessions.liveSessions(any()))
                .thenReturn(List.of(
                        reportedSessionBuilder()
                                .directory(otherProjectDirectory())
                                .status(ReportedStatus.UNKNOWN)
                                .build(),
                        reportedSession()));
        when(locator.locate(otherProjectDirectory())).thenReturn(new Project(Path.of("/work/other")));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("a session of another project has no say in whether this project's sessions can be shown")
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldSayWhyWhenNoSourceAnswers() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("unavailable sessions are shown as such, not a failed start")
                .isEqualTo(unavailableSessions());
    }

    @Test
    void shouldHandTheListenerTheSessionsReadAgainWhenTheRegistryChanges() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        sessionManager(sessions, locator, watch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(changed.capture(), any());

        changed.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheWatchFallsBackToTheSweep() {
        sessionManager(sessions, locator, watch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), fallback.capture());

        fallback.getValue().sweepingAlone(registryUnwatchable(), Duration.ofSeconds(2));

        verify(warnings)
                .raise(new Warning(
                        "sessions: cannot watch /home/max/.claude/sessions (User limit of inotify watches reached); checking every 2 s instead."));
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheSessionsComeFromClaudeAgents() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets());
        ArgumentCaptor<SourceFallback> fallback = ArgumentCaptor.forClass(SourceFallback.class);
        verify(sessions).liveSessions(fallback.capture());

        fallback.getValue().askingClaudeAgents(registryUnreadable(), Duration.ofSeconds(2));

        verify(warnings)
                .raise(new Warning("sessions: cannot read /home/max/.claude/sessions; asking claude agents --json every 2 s instead."));
    }

    @Test
    void shouldRaiseEachFallbacksWarningOnceHoweverOftenItIsReported() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        SessionManager manager = sessionManager(sessions, locator, watch, warnings);
        manager.sessionsFor(project(), noTickets());
        manager.sessionsFor(project(), noTickets());
        manager.watch(project(), noTickets(), listener);
        ArgumentCaptor<SourceFallback> source = ArgumentCaptor.forClass(SourceFallback.class);
        verify(sessions, times(2)).liveSessions(source.capture());
        ArgumentCaptor<WatchFallback> sweep = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), sweep.capture());

        source.getAllValues().forEach(fallback -> fallback.askingClaudeAgents(registryUnreadable(), Duration.ofSeconds(2)));
        sweep.getValue().sweepingAlone(registryUnwatchable(), Duration.ofSeconds(2));
        sweep.getValue().sweepingAlone(registryUnwatchable(), Duration.ofSeconds(2));

        assertThat(raisedTexts(warnings))
                .as("each fallback is named once, and one being named does not silence the other")
                .containsExactly(
                        "sessions: cannot read /home/max/.claude/sessions; asking claude agents --json every 2 s instead.",
                        "sessions: cannot watch /home/max/.claude/sessions (User limit of inotify watches reached); checking every 2 s instead.");
    }

    @Test
    void shouldRaiseNothingWhileTheSourcesAnswer() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets());
        sessionManager(sessions, locator, watch, warnings).watch(project(), noTickets(), listener);

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldListPastSessionsNewestActivityFirstAfterTheLiveOnes() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.pastSessions())
                .thenReturn(List.of(
                        locatedIn(pastSessionActiveMinutesAgo(2, 20)),
                        locatedIn(pastSessionActiveMinutesAgo(1, 10)),
                        locatedIn(pastSessionActiveMinutesAgo(3, 30))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(availableSessionsOf(
                        List.of(liveSession()),
                        List.of(
                                pastSessionActiveMinutesAgo(1, 10),
                                pastSessionActiveMinutesAgo(2, 20),
                                pastSessionActiveMinutesAgo(3, 30))));
    }

    @Test
    void shouldOrderPastSessionsOfTheSameActivityByIdSoTheRowsDoNotShuffle() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions())
                .thenReturn(List.of(
                        locatedIn(pastSessionActiveMinutesAgo(2, 10)), locatedIn(pastSessionActiveMinutesAgo(1, 10))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .isEqualTo(pastSessionsOf(List.of(pastSessionActiveMinutesAgo(1, 10), pastSessionActiveMinutesAgo(2, 10))));
    }

    @Test
    void shouldListASessionOnceInTheLiveBlockWhenItsTranscriptIsThereToo() {
        PastSession alsoLive = pastSessionBuilder().id(reportedSession().id()).build();
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.pastSessions()).thenReturn(List.of(locatedIn(alsoLive), locatedPastSession()));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .as("a live session with a transcript is live, not also past")
                .isEqualTo(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
    }

    @Test
    void shouldHideAPastSessionWhoseIdIsAnSdkCliSessionThatRuns() {
        PastSession sdk = pastSessionBuilder().id(sdkCliSession().id()).build();
        when(sessions.liveSessions(any())).thenReturn(List.of(sdkCliSession()));
        when(past.pastSessions()).thenReturn(List.of(locatedIn(sdk)));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .as("a session that runs is not past, whether or not it is one anyone works in")
                .isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldListAPastSessionGitLocatedInTheProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedIn(pastSessionIn(worktreeDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .as("a ticket worktree's transcript was located in the main worktree's project when it was scanned")
                .isEqualTo(pastSessionsOf(List.of(pastSessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListAPastSessionGitLocatedInAnotherProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions())
                .thenReturn(List.of(locatedPastSessionBuilder()
                        .location(new LocatedInProject(new Project(Path.of("/work/other"))))
                        .build()));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldListAPastSessionOfARemovedWorktreeByItsPath() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(notLocated(pastSessionIn(worktreeDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets()))
                .as("the directory is gone, so git cannot say, and ../<repo>.worktrees/ is where the project's worktrees were")
                .isEqualTo(pastSessionsOf(List.of(pastSessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListAPastSessionOfARemovedDirectoryElsewhere() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(notLocated(pastSessionIn(outsideGitDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets())).isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldNotLocateAnythingWhenReadingPastSessions() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));

        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets());

        verifyNoInteractions(locator);
    }

    @Test
    void shouldStillSayWhyWhenNoLiveSourceAnswersWhateverThePastSessionsAre() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets())).isEqualTo(unavailableSessions());
    }

    @Test
    void shouldReuseTheLastReadOfTheLiveSessionsWhenOnlyTheTranscriptsChange() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener, times(2)).accept(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
        verify(sessions, times(1)).liveSessions(any());
        verify(locator, times(1)).locate(any());
    }

    @Test
    void shouldReadTheLiveSessionsWhenTheTranscriptsChangeBeforeTheRegistryWasRead() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldReadTheLiveSessionsAgainWhenTheTranscriptsChangeAfterTheRegistryCouldNotBeRead() {
        when(sessions.liveSessions(any()))
                .thenReturn(List.of(reportedSession()))
                .thenThrow(sessionsUnavailable())
                .thenReturn(List.of());
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener)
                .accept(liveSessionsOf(List.of()));
    }

    @Test
    void shouldListALiveSessionOfARemovedWorktreeByItsPath() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(worktreeDirectory())));
        when(locator.locate(worktreeDirectory())).thenThrow(new ProjectNotFoundException("not a directory"));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets()))
                .as("a session still running in a removed worktree is the project's, as its past row would be")
                .isEqualTo(liveSessionsOf(List.of(sessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldRaiseOneWarningWhenTheTranscriptsRootCannotBeListed() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<RootFallback> root = ArgumentCaptor.forClass(RootFallback.class);
        verify(transcriptWatch).watch(any(), any(), any(), any(), root.capture());

        root.getValue().unlistable(transcriptsUnlistable());
        root.getValue().unlistable(transcriptsUnlistable());

        assertThat(raisedTexts(warnings))
                .containsExactly("past sessions: cannot list /home/max/.claude/projects: access denied to /home/max/.claude/projects; only those already read show.");
    }

    @Test
    void shouldLocateATranscriptInTheProjectGitFindsForItsDirectory() {
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThat(transcriptLocator.getValue().locate(workingDirectoryOf(reportedSession())))
                .isEqualTo(locatedInProject());
    }

    @Test
    void shouldLocateATranscriptGitCannotPlaceAsNotLocated() {
        when(locator.locate(outsideGitDirectory())).thenThrow(new ProjectNotFoundException("not a directory"));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThat(transcriptLocator.getValue().locate(outsideGitDirectory())).isEqualTo(new NotLocated());
    }

    @Test
    void shouldLetAFailedLookupReachTheWatcherSoTheTranscriptIsAskedAgain() {
        when(locator.locate(outsideGitDirectory()))
                .thenThrow(new ProjectUnavailableException(reason().value(), new InterruptedException()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThatThrownBy(() -> transcriptLocator.getValue().locate(outsideGitDirectory()))
                .as("a git that failed says nothing about where the transcript belongs, so it must not be cached as nowhere")
                .isInstanceOf(ProjectUnavailableException.class);
    }

    @Test
    void shouldRaiseAWarningNamingTheTreeAndHowOftenWhenTheTranscriptsAreOnlyRestated() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<PollingFallback> fallback = ArgumentCaptor.forClass(PollingFallback.class);
        verify(transcriptWatch).watch(any(), any(), fallback.capture(), any(), any());

        fallback.getValue().polling(transcriptsUnwatchable(), Duration.ofSeconds(5));
        fallback.getValue().polling(transcriptsUnwatchable(), Duration.ofSeconds(5));

        assertThat(raisedTexts(warnings))
                .as("named once, with the sysctl the adapter knows it depends on")
                .containsExactly(
                        "past sessions: cannot watch /home/max/.claude/projects (User limit of inotify watches reached); if the watch limit is reached, raise fs.inotify.max_user_watches; checking every 5 s instead.");
    }

    @Test
    void shouldRaiseOneWarningWhenTheCacheCannotBeWritten() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), listener);
        ArgumentCaptor<CacheFallback> cache = ArgumentCaptor.forClass(CacheFallback.class);
        verify(transcriptWatch).watch(any(), any(), any(), cache.capture(), any());

        cache.getValue().unwritable(cacheUnwritable());
        cache.getValue().unwritable(cacheUnwritable());

        assertThat(raisedTexts(warnings))
                .containsExactly(
                        "past sessions: cannot write the cache /home/max/.cache/lazyclaude/past-sessions.json: Permission denied; they are read in full on every start.");
    }

    @Test
    void shouldLinkALiveSessionThroughItsTranscriptFoundBySessionId() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets()))
                .isEqualTo(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldLeaveALiveSessionWithNoTranscriptYetUnlinked() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of());

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets()))
                .as("its branch cannot be known before its transcript exists")
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListALiveSessionLinkedToTwoTicketsWithBothLinks() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("review", "PROJ-1", "PROJ-2"))));

        AvailableSessions read = (AvailableSessions) sessionManager(sessions, past, locator, watch, transcriptWatch, warnings)
                .sessionsFor(project(), boardTickets());

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder(strong("PROJ-1", "review"), strong("PROJ-2", "review"));
    }

    @Test
    void shouldLinkAPastSessionWeaklyThroughItsBranch() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), onBranches("proj-2")));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets()))
                .isEqualTo(pastSessionsOf(List.of(pastSessionBuilder().links(List.of(weak("PROJ-2"))).build())));
    }

    @Test
    void shouldLeaveAPastSessionUnlinkedWhenItsArgsNamedOnlyTicketsTheBoardLacks() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ranOnBranch(ticketRun("start", "GONE-9"), "PROJ-2")));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldMoveALiveSessionUnderItsTicketOnceItsTranscriptNamesIt() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(), Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), boardTickets(), listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
        verify(listener).accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldMarkALiveSessionHostedWhenItsPidIsOneLazyclaudeSpawned() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        Sessions read = sessionManager(sessions, locator, watch, warnings, () -> hostedSessionsOf(hostedSession()))
                .sessionsFor(project(), noTickets());

        assertThat(read).isEqualTo(availableSessionsHosting(List.of(hostedLiveSession()), List.of(openLive(hostedLiveSession()))));
    }

    @Test
    void shouldLeaveALiveSessionOutsideWhenItsPidIsNotOneLazyclaudeSpawned() {
        when(sessions.liveSessions(any())).thenReturn(List.of(newestReportedSession()));
        when(locator.locate(workingDirectoryOf(newestReportedSession()))).thenReturn(project());

        AvailableSessions read = (AvailableSessions) sessionManager(sessions, locator, watch, warnings, () -> hostedSessionsOf(hostedSession()))
                .sessionsFor(project(), noTickets());

        assertThat(read.live()).extracting(LiveSession::hosting).containsExactly(Hosting.OUTSIDE);
    }

    @Test
    void shouldListAHostedSessionAsStartingWhileTheRegistryHasNoRecordOfIt() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        Sessions read = sessionManager(sessions, locator, watch, warnings, () -> hostedSessionsOf(hostedSession()))
                .sessionsFor(project(), noTickets());

        assertThat(read).isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }

    @Test
    void shouldReadWhichSessionsAreHostedOnEveryRead() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<HostedSessions> now = new ArrayList<>(List.of(hostedSessionsOf()));
        SessionManager manager = sessionManager(sessions, locator, watch, warnings, () -> now.getLast());
        manager.sessionsFor(project(), noTickets());
        now.add(hostedSessionsOf(hostedSession()));

        assertThat(manager.sessionsFor(project(), noTickets()))
                .as("a tab opened between two reads shows up in the second")
                .isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }

    @Test
    void shouldDeliverTheSessionsOnRefresh() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        sessionManager(sessions, locator, watch, warnings).refresh(project(), noTickets(), listener);

        verify(listener).accept(liveSessionsOf(List.of()));
    }

    @Test
    void shouldKeepTheHostedSessionsWhenNoSourceAnswers() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        Sessions read = sessionManager(sessions, locator, watch, warnings, () -> hostedSessionsOf(hostedSession()))
                .sessionsFor(project(), noTickets());

        assertThat(read)
                .as("a registry that cannot be read must not close the tabs of claudes that are running")
                .isEqualTo(unavailableSessionsHosting(List.of(openStarting())));
    }
}
