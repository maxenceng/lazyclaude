package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.sessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketStart;
import static dev.maxenceng.lazyclaude.session.domain.TicketWorktreeFixture.ticketWorktree;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.OpenHostedSession;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.StartingPhase;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.domain.UnavailableSessions;
import dev.maxenceng.lazyclaude.session.domain.UnreadPhase;
import dev.maxenceng.lazyclaude.session.domain.VerbChoice;
import dev.maxenceng.lazyclaude.session.domain.WorktreePort;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
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

    @Test
    void shouldReadTheLiveSessionsOfTheProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession())))
                .thenReturn(project());

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).sessionsFor(project(), noTickets()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldHandTheListenerWhyAndKeepWatchingWhenARereadCannotAnswer() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable()).thenReturn(List.of());
        List<Sessions> received = new ArrayList<>();

        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).subscribe(project(), noTickets(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received)
                .as("the panel says why, and the next sweep reads again")
                .containsExactly(unavailableSessions(), liveSessionsOf(List.of()));
    }

    @Test
    void shouldReadThePastSessionsOfTheProjectToo() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).sessionsFor(project(), noTickets()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldHandTheListenerTheSessionsAgainWhenTheTranscriptWatcherReportsAChange() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<Sessions> received = new ArrayList<>();

        sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).subscribe(project(), noTickets(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), changed.capture(), any(), any(), any());
        changed.getValue().run();

        assertThat(received)
                .as("the transcripts are read on their own thread, so their landing must reach the panel without waiting for the registry")
                .containsExactly(liveSessionsOf(List.of()));
    }

    @Test
    void shouldLinkTheLiveSessionsToTheTicketsItIsHanded() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts).sessionsFor(project(), boardTickets()))
                .isEqualTo(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldStartAnUnlinkedSessionThroughTheHost() {
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts)
                .startUnlinked(project(), paneSize());

        assertThat(start).isInstanceOf(StartedHost.class);
    }

    @Test
    void shouldStartATicketSessionThroughTheHostInTheWorktreeOfItsBranch() {
        when(worktrees.checkedOutAt(project(), branchName())).thenReturn(Optional.of(worktreeDirectory()));
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).startTicket(ticketStart(Verb.REVIEW), paneSize());

        assertThat(start).isInstanceOf(StartedHost.class);
    }

    @Test
    void shouldResumeATicketSessionThroughTheHost() {
        when(hosts.exists(worktreeDirectory())).thenReturn(true);
        when(hosts.spawn(any(), any())).thenReturn(processId());

        HostStart start = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).resumeInTicketWorktree(sessionId(), worktreeDirectory(), ticketWorktree(), paneSize());

        assertThat(start).isInstanceOf(StartedHost.class);
    }

    @Test
    void shouldPreselectDesignWhenNoSessionHasRunItOnTheTicket() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false))
                .isEqualTo(VerbChoice.builder().verbs(List.of(Verb.REVIEW, Verb.DESIGN)).preselected(Verb.DESIGN).build());
    }

    @Test
    void shouldPreselectReviewWhenAPastSessionRanDesignOnTheTicket() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ran(ticketRun("design", "PROJ-1"))));

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                        .preselected())
                .isEqualTo(Verb.REVIEW);
    }

    @Test
    void shouldPreselectDoneWhenTheBranchIsMerged() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.RESPOND, Verb.DONE), true)
                        .preselected())
                .isEqualTo(Verb.DONE);
    }

    @Test
    void shouldAnswerTheChoiceFromTheLastPushWithoutReadingTheRegistryAgain() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSession().id(), ran(ticketRun("design", "PROJ-1"))));
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees);
        service.subscribe(project(), boardTickets(), read -> {});
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(changed.capture(), any());
        changed.getValue().run();

        Verb preselected = service.verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                .preselected();

        assertThat(preselected).as("the pushed sessions say design ran").isEqualTo(Verb.REVIEW);
        verify(sessions, times(1)).liveSessions(any());
    }

    @Test
    void shouldNotLetAnUnreadableRegistryStopTheChoice() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        assertThat(sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts, worktrees).verbChoiceFor(project(), boardTickets(), linkedTicketId(), List.of(Verb.REVIEW, Verb.DESIGN), false)
                        .preselected())
                .isEqualTo(Verb.DESIGN);
    }

    @Test
    void shouldHandTheListenerTheSessionsAgainWhenAHostedSessionOpens() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(hosts.spawn(any(), any())).thenReturn(processId());
        List<Sessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.startUnlinked(project(), paneSize());

        assertThat(received)
                .as("the registry has not heard of the new claude yet, so only lazyclaude can say a tab opened")
                .singleElement()
                .isInstanceOfSatisfying(AvailableSessions.class, available -> assertThat(available.hosted()).hasSize(1));
    }

    @Test
    void shouldListAHostedSessionAsStartingBeforeTheRegistryHasIt() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(hosts.spawn(any(), any())).thenReturn(processId());
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.startUnlinked(project(), paneSize());

        assertThat(service.sessionsFor(project(), noTickets()))
                .isInstanceOfSatisfying(
                        AvailableSessions.class,
                        available -> assertThat(available.hosted()).extracting(OpenHostedSession::phase).containsExactly(new StartingPhase()));
    }

    @Test
    void shouldCountAHostedSessionAsBusyWhileTheRegistryCannotBeRead() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());
        when(hosts.spawn(any(), any())).thenReturn(processId());
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.startUnlinked(project(), paneSize());

        assertThat(service.sessionsFor(project(), noTickets()))
                .as("quitting must ask while nothing says whether a hosted claude is mid-work")
                .isInstanceOfSatisfying(
                        UnavailableSessions.class,
                        unavailable -> assertThat(unavailable.hosted()).extracting(OpenHostedSession::phase).containsExactly(new UnreadPhase()));
    }

    @Test
    void shouldEndEveryHostedSessionOnQuit() {
        when(hosts.spawn(any(), any())).thenReturn(processId());
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        HostedSessionId id = ((StartedHost) service.startUnlinked(project(), paneSize())).id();

        service.endAll();

        verify(hosts).end(id);
    }

    @Test
    void shouldHandTheListenerTheSessionsReadAgainAndAskForTheTranscriptsAgainOnRefresh() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<Sessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.refresh(project(), noTickets());

        assertThat(received).containsExactly(liveSessionsOf(List.of()));
        verify(transcriptWatch).rescanNow();
    }

    @Test
    void shouldHandTheListenerTheSessionsLinkedAgainstTheTicketsItWasToldOf() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        List<Sessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.knowTickets(project(), boardTickets());

        assertThat(received)
                .containsExactly(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldHandTheListenerTheSessionsLinkedAgainstTheTicketsItIsRefreshedWith() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(liveSession().id(), ran(ticketRun("start", "PROJ-1"))));
        List<Sessions> received = new ArrayList<>();
        SessionApplicationService service = sessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, hosts);
        service.subscribe(project(), noTickets(), received::add);

        service.refresh(project(), boardTickets());

        assertThat(received)
                .containsExactly(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }
}
