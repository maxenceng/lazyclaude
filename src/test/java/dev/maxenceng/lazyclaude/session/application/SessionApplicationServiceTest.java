package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.hostingTheRegisteredSession;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationServiceWithoutDesktopNotifications;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.inputNotification;
import static dev.maxenceng.lazyclaude.session.domain.DesktopNotificationFixture.notificationId;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWaitingFor;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWith;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.recordUnreadable;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.AvailableSessions;
import dev.maxenceng.lazyclaude.session.domain.DesktopNotificationPort;
import dev.maxenceng.lazyclaude.session.domain.HostLaunch;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.OpenHostedSession;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.StartingPhase;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.UnreadPhase;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;
import dev.maxenceng.lazyclaude.session.domain.WatchedSession;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionApplicationServiceTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private PastSessionPort past;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private HostPort hosts;

    @Mock
    private WorktreePort worktrees;

    @Mock
    private DesktopNotificationPort notifications;

    @Test
    void shouldReadTheStateOfTheHostedSessionsFromTheirRecords() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        SessionApplicationService service = hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts), hosts);

        AvailableSessions read = service.sessionsFor(project(), noTickets());

        assertThat(read.live()).containsExactly(liveSessionBuilder().build());
    }

    @Test
    void shouldReadNoRecordWhenNoSessionIsHosted() {
        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts)
                        .sessionsFor(project(), noTickets())
                        .live())
                .as("lazyclaude lists only the sessions it hosts, so a claude in another terminal is never looked for")
                .isEmpty();
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldKeepWatchingAndShowTheSessionAsUnknownWhileItsRecordCannotBeRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable()).thenReturn(Optional.of(reportedSession()));
        List<AvailableSessions> received = new ArrayList<>();
        SessionApplicationService service = hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts), hosts);

        service.subscribe(project(), noTickets(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received)
                .as("the tab says unknown until the next sweep reads the record again")
                .extracting(read -> read.live().getFirst().state())
                .containsExactly(new Unknown(), new Busy());
    }

    @Test
    void shouldReadThePastSessionsOfTheProjectToo() {
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).sessionsFor(project(), noTickets()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldHandTheListenerTheSessionsAgainWhenTheTranscriptWatcherReportsAChange() {
        List<AvailableSessions> received = new ArrayList<>();

        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).subscribe(project(), noTickets(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), changed.capture(), any(), any(), any());
        changed.getValue().run();

        assertThat(received)
                .as("the transcripts are read on their own thread, so their landing must reach the panel without waiting for the registry")
                .containsExactly(liveSessionsOf(List.of()));
    }

    @Test
    void shouldLinkTheHostedSessionsToTheTicketsItIsHanded() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        when(past.linkFacts()).thenReturn(Map.of(id, ran(ticketRun("start", "PROJ-1"))));

        assertThat(service.sessionsFor(project(), boardTickets()).live())
                .containsExactly(liveSessionBuilder().id(id).links(List.of(strong("PROJ-1", "start"))).build());
    }

    @Test
    void shouldStartAnUnlinkedSessionThroughTheHost() {
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts)
                .startUnlinked(project(), paneSize());

        assertThat(start).isInstanceOf(StartedHost.class);
    }

    @Test
    void shouldLaunchANewSessionUnderTheIdItsTabCarries() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        ArgumentCaptor<HostLaunch> launched = ArgumentCaptor.forClass(HostLaunch.class);

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts)
                .startUnlinked(project(), paneSize());
        verify(hosts).spawn(launched.capture(), any());

        assertThat(launched.getValue().command().session())
                .as("claude --session-id is given the id the row, the tab and the notification are named by")
                .isEqualTo(((StartedHost) start).id());
    }

    @Test
    void shouldStartATicketSessionThroughTheHostInTheWorktreeOfItsBranch() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(start).isInstanceOf(StartedHost.class);
    }

    @Test
    void shouldResumeATicketSessionThroughTheHostUnderTheIdItResumes() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        assertThat(start).isEqualTo(new StartedHost(sessionId()));
    }

    @Test
    void shouldPreselectDesignWhenNoSessionHasRunItOnTheTicket() {
        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false))
                .isEqualTo(VerbChoice.builder().verbs(List.of(Verb.REVIEW, Verb.DESIGN)).preselected(Verb.DESIGN).build());
    }

    @Test
    void shouldPreselectReviewWhenAPastSessionRanDesignOnTheTicket() {
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ran(ticketRun("design", "PROJ-1"))));

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                        .preselected())
                .isEqualTo(Verb.REVIEW);
    }

    @Test
    void shouldPreselectDoneWhenTheBranchIsMerged() {
        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.RESPOND, Verb.DONE), true)
                        .preselected())
                .isEqualTo(Verb.DONE);
    }

    @Test
    void shouldAnswerTheChoiceFromTheLastPushWithoutReadingTheRecordsAgain() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ran(ticketRun("design", "PROJ-1"))));
        SessionApplicationService service = hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees), hosts);
        service.subscribe(project(), boardTickets(), read -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();

        Verb preselected = service.verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                .preselected();

        assertThat(preselected).as("the pushed sessions say design ran").isEqualTo(Verb.REVIEW);
        verify(sessions, times(1)).recordOf(processId());
    }

    @Test
    void shouldNotLetAnUnreadableRecordStopTheChoice() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        SessionApplicationService service = hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees), hosts);

        assertThat(service.verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                        .preselected())
                .isEqualTo(Verb.DESIGN);
    }

    @Test
    void shouldHandTheListenerTheSessionsAgainWhenAHostedSessionOpens() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        List<AvailableSessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.startUnlinked(project(), paneSize());

        assertThat(received)
                .as("the registry has not heard of the new claude yet, so only lazyclaude can say a tab opened")
                .singleElement()
                .satisfies(available -> assertThat(available.hosted()).hasSize(1));
    }

    @Test
    void shouldListAHostedSessionAsStartingBeforeItsRecordIsWritten() {
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        hostingTheRegisteredSession(service, hosts);

        assertThat(service.sessionsFor(project(), noTickets()).hosted())
                .extracting(OpenHostedSession::phase)
                .containsExactly(new StartingPhase());
    }

    @Test
    void shouldCountAHostedSessionAsBusyWhileItsRecordCannotBeRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        hostingTheRegisteredSession(service, hosts);

        assertThat(service.sessionsFor(project(), noTickets()).hosted())
                .as("quitting must ask while nothing says whether a hosted claude is mid-work")
                .singleElement()
                .satisfies(open -> assertThat(open.phase()).isInstanceOf(UnreadPhase.class))
                .satisfies(open -> assertThat(open.phase().isBusyOrWaiting()).isTrue());
    }

    @Test
    void shouldEndEveryHostedSessionOnQuit() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        SessionId id = ((StartedHost) service.startUnlinked(project(), paneSize())).id();

        service.endAll();

        verify(hosts).end(id);
    }

    @Test
    void shouldHandTheListenerTheSessionsReadAgainAndAskForTheTranscriptsAgainOnRefresh() {
        List<AvailableSessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.refresh(project(), noTickets());

        assertThat(received).containsExactly(liveSessionsOf(List.of()));
        verify(transcriptWatch).rescanNow();
    }

    @Test
    void shouldHandTheListenerTheSessionsLinkedAgainstTheTicketsItWasToldOf() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        List<AvailableSessions> received = new ArrayList<>();
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        when(past.linkFacts()).thenReturn(Map.of(id, ran(ticketRun("start", "PROJ-1"))));
        service.subscribe(project(), noTickets(), received::add);

        service.knowTickets(project(), boardTickets());

        assertThat(received).singleElement().satisfies(linked -> assertThat(linked.live())
                .containsExactly(liveSessionBuilder().id(id).links(List.of(strong("PROJ-1", "start"))).build()));
    }

    @Test
    void shouldHandTheListenerTheSessionsLinkedAgainstTheTicketsItIsRefreshedWith() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        List<AvailableSessions> received = new ArrayList<>();
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        when(past.linkFacts()).thenReturn(Map.of(id, ran(ticketRun("start", "PROJ-1"))));
        service.subscribe(project(), noTickets(), received::add);

        service.refresh(project(), boardTickets());

        assertThat(received).singleElement().satisfies(linked -> assertThat(linked.live())
                .containsExactly(liveSessionBuilder().id(id).links(List.of(strong("PROJ-1", "start"))).build()));
    }

    @Test
    void shouldNotifyWhenASweepFindsAHostedSessionEnteringWaiting() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        when(notifications.show(inputNotification())).thenReturn(notificationId());
        hostingTheRegisteredSession(
                        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications), hosts)
                .subscribe(project(), noTickets(), received -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());

        changed.getValue().run();
        changed.getValue().run();

        verify(notifications).show(inputNotification());
    }

    @Test
    void shouldNotNotifyAClaudeRunningInAnotherTerminalEnteringWaiting() {
        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, notifications)
                .subscribe(project(), noTickets(), received -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());

        changed.getValue().run();
        changed.getValue().run();

        verify(notifications, never()).show(any());
        verifyNoInteractions(sessions);
    }

    @Test
    void shouldHandTheListenerTheSameWaitingSessionsWhetherDesktopNotificationsAreOnOrOff() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        List<AvailableSessions> withNotifications = new ArrayList<>();
        List<AvailableSessions> withoutNotifications = new ArrayList<>();
        hostingTheRegisteredSession(
                        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications), hosts)
                .subscribe(project(), noTickets(), withNotifications::add);
        hostingTheRegisteredSession(
                        sessionApplicationServiceWithoutDesktopNotifications(
                                sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications),
                        hosts)
                .subscribe(project(), noTickets(), withoutNotifications::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch, times(2)).watch(any(), changed.capture(), any());

        changed.getAllValues().forEach(Runnable::run);

        assertThat(withoutNotifications.stream().map(pushed -> pushed.live().getFirst().state()).toList())
                .as("markers and the status line read these sessions, not the notifications")
                .isNotEmpty()
                .isEqualTo(withNotifications.stream().map(pushed -> pushed.live().getFirst().state()).toList());
    }

    @Test
    void shouldNotNotifyWhenDesktopNotificationsAreOff() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        hostingTheRegisteredSession(
                        sessionApplicationServiceWithoutDesktopNotifications(
                                sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications),
                        hosts)
                .subscribe(project(), noTickets(), received -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());

        changed.getValue().run();
        changed.getValue().run();

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldNotNotifyAgainWhenARefreshHandsOverTheSameStates() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        SessionApplicationService service = hostingTheRegisteredSession(
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications), hosts);
        service.subscribe(project(), noTickets(), received -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();

        service.refresh(project(), noTickets());
        service.knowTickets(project(), boardTickets());

        verifyNoInteractions(notifications);
    }

    @Test
    void shouldCloseTheNotificationOfTheSessionThatBecomesWatched() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        when(notifications.show(any())).thenReturn(notificationId());
        SessionApplicationService service =
                sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, notifications);
        hostingTheRegisteredSession(service, hosts);
        SessionId id = sessionId();
        service.subscribe(project(), noTickets(), received -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        service.watching(new WatchedSession(id));

        verify(notifications).close(notificationId());
    }
}
