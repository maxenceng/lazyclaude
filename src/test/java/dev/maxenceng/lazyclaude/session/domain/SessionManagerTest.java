package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.noneHosted;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.oneHosted;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.twoHosted;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.onBranches;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ranOnBranch;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
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
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWaitingFor;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWith;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.advancingClock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.hostingSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.recordUnreadable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionManager;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.cacheUnwritable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnlistable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
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
    private Consumer<AvailableSessions> listener;

    @Test
    void shouldListAHostedSessionAsTheLiveSessionItsRecordDescribes() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldNameTheLiveSessionByTheIdItWasLaunchedWith() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = sessionManager(sessions, locator, watch, warnings)
                .sessionsFor(project(), noTickets(), oneHosted());

        assertThat(read.live())
                .as("the row carries the id lazyclaude chose, which no registry record has to repeat")
                .singleElement()
                .extracting(LiveSession::id)
                .isEqualTo(hostedSession().id());
    }

    @Test
    void shouldReadOnlyTheRecordsOfTheProcessesItHosts() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        sessionManager(sessions, locator, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        verify(sessions).recordOf(processId());
        verify(sessions).recordOf(secondProcessId());
        verifyNoInteractions(locator);
    }

    @Test
    void shouldListAHostedSessionWithNoRecordYetAsStarting() {
        when(sessions.recordOf(processId())).thenReturn(Optional.empty());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a claude on its trust dialog has written no record, and that is not a failure")
                .isEqualTo(hostingSessions(List.of(openStarting())));
        verifyNoInteractions(warnings);
    }

    @Test
    void shouldListAHostedSessionWhoseRecordCannotBeReadAsUnknownWithOneWarningNamingWhy() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(hostingSessions(List.of(openUnread())));
        assertThat(raisedTexts(warnings))
                .containsExactly("hosted session 70c67de2: %d.json does not parse: Unexpected end-of-input; its state is unknown.".formatted(processId().value()));
    }

    @Test
    void shouldKeepTheStateOfTheOtherSessionsWhenOneRecordCannotBeRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        AvailableSessions read = sessionManager(sessions, locator, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        assertThat(read.hosted())
                .extracting(open -> open.phase().live().map(LiveSession::state))
                .containsExactly(Optional.of(new Unknown()), Optional.of(new Busy()));
    }

    @Test
    void shouldWarnOnceHoweverOftenAnUnreadableRecordIsRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        SessionManager manager = sessionManager(sessions, locator, watch, warnings);

        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());

        assertThat(raisedTexts(warnings)).hasSize(1);
    }

    @Test
    void shouldWarnAgainWhenARecordFailsAfterItWasReadAgain() {
        when(sessions.recordOf(processId()))
                .thenThrow(recordUnreadable())
                .thenReturn(Optional.of(reportedSession()))
                .thenThrow(recordUnreadable());
        SessionManager manager = sessionManager(sessions, locator, watch, warnings);

        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());

        assertThat(raisedTexts(warnings)).hasSize(2);
    }

    @Test
    void shouldLinkATicketLaunchToItsTicketBeforeItsTranscriptExists() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = sessionManager(sessions, locator, watch, warnings)
                .sessionsFor(project(), boardTickets(), hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(strong("PROJ-1", "design"));
    }

    @Test
    void shouldMeasureEveryAgeOfOneReadFromTheSameInstant() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = new SessionManager(sessions, mock(PastSessionPort.class), locator, watch, mock(TranscriptWatchPort.class), warnings, advancingClock())
                .sessionsFor(project(), noTickets(), oneHosted());

        assertThat(read.ageOf(liveSession()))
                .as("rows of one read drawn at different instants would disagree on how long ago they started")
                .isEqualTo(read.ageOf(liveSession()));
    }

    @Test
    void shouldOrderNewestStartFirst() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        AvailableSessions read = sessionManager(sessions, locator, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        assertThat(read.live()).containsExactly(newestSession(), liveSession());
    }

    @Test
    void shouldKeepTheOrderWhenAStateChanges() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.IDLE)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));
        SessionManager manager = sessionManager(sessions, locator, watch, warnings);

        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live()).containsExactly(newestSession(), sessionWith(new Busy()));
        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live()).containsExactly(newestSession(), sessionWith(new YourTurn()));
        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live())
                .containsExactly(newestSession(), sessionWith(new WaitingForInput(inputNeeded())));
    }

    @Test
    void shouldBeBusyWhenTheStatusIsShell() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWith(ReportedStatus.SHELL)));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Busy()))));
    }

    @Test
    void shouldWaitForPermissionOnAPermissionPrompt() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(permissionPrompt())));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(waitingForPermission()))));
    }

    @Test
    void shouldWaitForInputWhenNoReasonIsGiven() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(new NoWaitingReason())));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new WaitingForInput(new NoWaitingReason())))));
    }

    @Test
    void shouldIgnoreAReasonOnASessionThatIsNotWaiting() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedSessionBuilder()
                        .status(ReportedStatus.IDLE)
                        .waitingFor(permissionPrompt())
                        .build()));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("only a waiting session is blocked, whatever stale reason its record carries")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new YourTurn()))));
    }

    @Test
    void shouldListASessionWhoseStatusItDoesNotKnowAsUnknown() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWith(ReportedStatus.UNKNOWN)));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a newer Claude Code's status must not hide a session that runs")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Unknown()))));
    }

    @Test
    void shouldHandTheListenerTheSessionsReadAgainWhenTheRegistryChanges() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());

        changed.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldWatchOnlyTheRecordsOfTheProcessesItHosts() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Predicate<ProcessId>> hosted = ArgumentCaptor.captor();
        verify(watch).watch(hosted.capture(), any(), any());

        assertThat(hosted.getValue().test(processId())).isTrue();
        assertThat(hosted.getValue().test(secondProcessId()))
                .as("a claude in another terminal is nobody's business here")
                .isFalse();
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheWatchFallsBackToTheSweep() {
        sessionManager(sessions, locator, watch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), any(), fallback.capture());

        fallback.getValue().sweepingAlone(registryUnwatchable(), Duration.ofSeconds(2));

        verify(warnings)
                .raise(new Warning(
                        "sessions: cannot watch /home/max/.claude/sessions (User limit of inotify watches reached); checking every 2 s instead."));
    }

    @Test
    void shouldRaiseNothingWhileTheRecordsAreRead() {
        sessionManager(sessions, locator, watch, warnings).sessionsFor(project(), noTickets(), noneHosted());
        sessionManager(sessions, locator, watch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldListPastSessionsNewestActivityFirstAfterTheLiveOnes() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.pastSessions())
                .thenReturn(List.of(
                        locatedIn(pastSessionActiveMinutesAgo(2, 20)),
                        locatedIn(pastSessionActiveMinutesAgo(1, 10)),
                        locatedIn(pastSessionActiveMinutesAgo(3, 30))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(availableSessionsOf(
                        List.of(liveSession()),
                        List.of(
                                pastSessionActiveMinutesAgo(1, 10),
                                pastSessionActiveMinutesAgo(2, 20),
                                pastSessionActiveMinutesAgo(3, 30))));
    }

    @Test
    void shouldOrderPastSessionsOfTheSameActivityByIdSoTheRowsDoNotShuffle() {
        when(past.pastSessions())
                .thenReturn(List.of(
                        locatedIn(pastSessionActiveMinutesAgo(2, 10)), locatedIn(pastSessionActiveMinutesAgo(1, 10))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .isEqualTo(pastSessionsOf(List.of(pastSessionActiveMinutesAgo(1, 10), pastSessionActiveMinutesAgo(2, 10))));
    }

    @Test
    void shouldListASessionOnceInTheLiveBlockWhenItsTranscriptIsThereToo() {
        PastSession alsoLive = pastSessionBuilder().id(hostedSession().id()).build();
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.pastSessions()).thenReturn(List.of(locatedIn(alsoLive), locatedPastSession()));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a live session with a transcript is live, not also past")
                .isEqualTo(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
    }

    @Test
    void shouldHideThePastSessionOfAHostedSessionStillStarting() {
        PastSession resumed = pastSessionBuilder().id(hostedSession().id()).build();
        when(sessions.recordOf(processId())).thenReturn(Optional.empty());
        when(past.pastSessions()).thenReturn(List.of(locatedIn(resumed)));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a resumed session shows once, as its tab, before Claude Code has rewritten its record")
                .isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }

    @Test
    void shouldListAPastSessionGitLocatedInTheProject() {
        when(past.pastSessions()).thenReturn(List.of(locatedIn(pastSessionIn(worktreeDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .as("a ticket worktree's transcript was located in the main worktree's project when it was scanned")
                .isEqualTo(pastSessionsOf(List.of(pastSessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListAPastSessionGitLocatedInAnotherProject() {
        when(past.pastSessions())
                .thenReturn(List.of(locatedPastSessionBuilder()
                        .location(new LocatedInProject(new Project(Path.of("/work/other"))))
                        .build()));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldListAPastSessionOfARemovedWorktreeByItsPath() {
        when(past.pastSessions()).thenReturn(List.of(notLocated(pastSessionIn(worktreeDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .as("the directory is gone, so git cannot say, and ../<repo>.worktrees/ is where the project's worktrees were")
                .isEqualTo(pastSessionsOf(List.of(pastSessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListAPastSessionOfARemovedDirectoryElsewhere() {
        when(past.pastSessions()).thenReturn(List.of(notLocated(pastSessionIn(outsideGitDirectory()))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldNotLocateAnythingWhenReadingPastSessions() {
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));

        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted());

        verifyNoInteractions(locator);
    }

    @Test
    void shouldReuseTheLastReadOfTheHostedSessionsWhenOnlyTheTranscriptsChange() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener, times(2)).accept(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
        verify(sessions, times(1)).recordOf(processId());
    }

    @Test
    void shouldReadTheRecordsWhenTheTranscriptsChangeBeforeTheyWereRead() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldRaiseOneWarningWhenTheTranscriptsRootCannotBeListed() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<RootFallback> root = ArgumentCaptor.forClass(RootFallback.class);
        verify(transcriptWatch).watch(any(), any(), any(), any(), root.capture());

        root.getValue().unlistable(transcriptsUnlistable());
        root.getValue().unlistable(transcriptsUnlistable());

        assertThat(raisedTexts(warnings))
                .containsExactly("past sessions: cannot list /home/max/.claude/projects: access denied to /home/max/.claude/projects; only those already read show.");
    }

    @Test
    void shouldLocateATranscriptInTheProjectGitFindsForItsDirectory() {
        when(locator.locate(projectRootDirectory())).thenReturn(project());
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThat(transcriptLocator.getValue().locate(projectRootDirectory())).isEqualTo(locatedInProject());
    }

    @Test
    void shouldLocateATranscriptGitCannotPlaceAsNotLocated() {
        when(locator.locate(outsideGitDirectory())).thenThrow(new ProjectNotFoundException("not a directory"));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThat(transcriptLocator.getValue().locate(outsideGitDirectory())).isEqualTo(new NotLocated());
    }

    @Test
    void shouldLetAFailedLookupReachTheWatcherSoTheTranscriptIsAskedAgain() {
        when(locator.locate(outsideGitDirectory()))
                .thenThrow(new ProjectUnavailableException(reason().value(), new InterruptedException()));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<TranscriptLocator> transcriptLocator = ArgumentCaptor.forClass(TranscriptLocator.class);
        verify(transcriptWatch).watch(transcriptLocator.capture(), any(), any(), any(), any());

        assertThatThrownBy(() -> transcriptLocator.getValue().locate(outsideGitDirectory()))
                .as("a git that failed says nothing about where the transcript belongs, so it must not be cached as nowhere")
                .isInstanceOf(ProjectUnavailableException.class);
    }

    @Test
    void shouldRaiseAWarningNamingTheTreeAndHowOftenWhenTheTranscriptsAreOnlyRestated() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
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
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
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
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldLeaveALiveSessionWithNoTranscriptYetUnlinked() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts()).thenReturn(Map.of());

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListALiveSessionLinkedToTwoTicketsWithBothLinks() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("review", "PROJ-1", "PROJ-2"))));

        AvailableSessions read = sessionManager(sessions, past, locator, watch, transcriptWatch, warnings)
                .sessionsFor(project(), boardTickets(), oneHosted());

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder(strong("PROJ-1", "review"), strong("PROJ-2", "review"));
    }

    @Test
    void shouldLinkAPastSessionWeaklyThroughItsBranch() {
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), onBranches("proj-2")));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), noneHosted()))
                .isEqualTo(pastSessionsOf(List.of(pastSessionBuilder().links(List.of(weak("PROJ-2"))).build())));
    }

    @Test
    void shouldLeaveAPastSessionUnlinkedWhenItsArgsNamedOnlyTicketsTheBoardLacks() {
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ranOnBranch(ticketRun("start", "GONE-9"), "PROJ-2")));

        assertThat(sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), noneHosted()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldMoveALiveSessionUnderItsTicketOnceItsTranscriptNamesIt() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts())
                .thenReturn(Map.of())
                .thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).watch(project(), boardTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
        verify(listener).accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldDeliverTheSessionsOnRefresh() {
        sessionManager(sessions, locator, watch, warnings).refresh(project(), noneHosted(), listener);

        verify(listener).accept(liveSessionsOf(List.of()));
    }

    @Test
    void shouldLinkASessionToATicketLearnedAfterTheWatchStarted() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        SessionManager manager = sessionManager(sessions, past, locator, watch, transcriptWatch, warnings);
        manager.watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);

        manager.know(project(), boardTickets(), oneHosted(), listener);

        verify(listener)
                .accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldKeepLinkingAgainstTheTicketsLearnedOnLaterChangesOfTheRegistry() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        SessionManager manager = sessionManager(sessions, past, locator, watch, transcriptWatch, warnings);
        manager.watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        manager.know(project(), boardTickets(), oneHosted(), listener);

        registryChanged.getValue().run();

        verify(listener, times(2))
                .accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldAskTheTranscriptWatchToRescanWhenTranscriptsAreRescanned() {
        sessionManager(sessions, past, locator, watch, transcriptWatch, warnings).rescanTranscripts();

        verify(transcriptWatch).rescanNow();
    }
}
