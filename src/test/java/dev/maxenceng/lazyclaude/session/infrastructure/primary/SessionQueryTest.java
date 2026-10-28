package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLog;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.factsRunning;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.interruptedLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.noTranscripts;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionId;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFacts;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFactsActiveMinutesAgo;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFactsRunning;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.transcriptsRead;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.recordUnreadable;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.verbChoicePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQueryHosting;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQueryHostingTheRegistered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.liveViewsOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketIds;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TerminalScreenFixture.screenSaying;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LaunchLogPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionQueryTest {

    private static final SessionPayload OF_THE_PROJECT = new SessionPayload(project().root(), List.of());
    private static final SessionPayload WITH_TICKETS = new SessionPayload(project().root(), linkedTicketIds("PROJ-1", "PROJ-2"));

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private PastSessionPort past;

    @Mock
    private LaunchLogPort launchLog;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private WarningPort warnings;

    @BeforeEach
    void setUp() {
        lenient().when(launchLog.read(any(), any())).thenReturn(emptyLaunchLog());
        lenient().when(past.transcriptsOf(any())).thenReturn(noTranscripts());
    }

    @Test
    void shouldViewNoSessionsWhenNoneIsHosted() {
        assertThat(sessionQuery(sessions, launchLog, watch, warnings).sessionsFor(OF_THE_PROJECT)).isEqualTo(emptySessionsView());
    }

    @Test
    void shouldViewTheHostedSessionWithItsAge() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(liveViewsOf(sessionQueryHostingTheRegistered(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT)))
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactly(sessionView());
    }

    @Test
    void shouldViewAHostedSessionWhoseRecordCannotBeReadAsUnknown() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());

        assertThat(liveViewsOf(sessionQueryHostingTheRegistered(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT)))
                .extracting(LiveSessionView::state)
                .containsExactly(new Unknown());
    }

    @Test
    void shouldHandTheListenerAViewWheneverTheWatcherReportsAChange() {
        List<SessionsView> received = new ArrayList<>();

        sessionQuery(sessions, launchLog, watch, warnings).subscribe(OF_THE_PROJECT, received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received).containsExactly(emptySessionsView(), emptySessionsView());
    }

    @Test
    void shouldViewEachPastSessionWithTheTimeSinceItsLastActivityNewestFirst() {
        when(launchLog.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(3), pastLaunchEntry(1)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFactsActiveMinutesAgo(3, 30), pastTranscriptFactsActiveMinutesAgo(1, 10)));

        assertThat(sessionQuery(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(pastSessionsViewOf(List.of(pastSessionViewNumbered(1), pastSessionViewNumbered(3))));
    }

    @Test
    void shouldViewThePastSessionsAfterTheLiveOnes() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(launchLog.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));

        SessionsView view = sessionQueryHostingTheRegistered(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT);

        assertThat(liveViewsOf(view)).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id").containsExactly(sessionView());
        assertThat(view.past()).containsExactly(pastSessionView());
    }

    @Test
    void shouldViewTheLinksOfALiveSessionSpeltAsTheBoardSpellsThem() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        SessionQuery query = sessionQueryHostingTheRegistered(sessions, past, launchLog, watch, transcriptWatch, warnings);
        SessionId id = sessionId();
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(factsRunning(id, ticketRun("start", "proj-1"))));

        assertThat(liveViewsOf(query.sessionsFor(WITH_TICKETS)))
                .containsExactly(sessionViewBuilder().id(id).links(List.of(ticketLink("PROJ-1", "start"))).build());
    }

    @Test
    void shouldViewTheLinksOfAPastSession() {
        when(launchLog.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFactsRunning(pastSessionId(1), ticketRun("review", "PROJ-2"))));

        assertThat(sessionQuery(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(WITH_TICKETS))
                .isEqualTo(pastSessionsViewOf(List.of(pastSessionViewBuilder().links(List.of(ticketLink("PROJ-2", "review"))).build())));
    }

    @Test
    void shouldViewAPastSessionWithNoRecordedEndAsInterrupted() {
        when(launchLog.read(eq(project()), any())).thenReturn(launchLogOf(interruptedLaunchEntry(1)));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));

        assertThat(sessionQuery(sessions, past, launchLog, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT).past())
                .singleElement()
                .extracting(PastSessionView::interrupted)
                .isEqualTo(true);
    }

    @Test
    void shouldViewNoLinkWhenThePayloadHandsOverNoTicket() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        SessionQuery query = sessionQueryHostingTheRegistered(sessions, past, launchLog, watch, transcriptWatch, warnings);
        SessionId id = sessionId();
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(factsRunning(id, ticketRun("start", "PROJ-1"))));

        assertThat(liveViewsOf(query.sessionsFor(OF_THE_PROJECT)))
                .containsExactly(sessionViewBuilder().id(id).build());
    }

    @Test
    void shouldAnswerWithTheScreenTheHostHolds() {
        HostPort hosts = mock(HostPort.class);
        when(hosts.screenOf(sessionId())).thenReturn(screenSaying("hello"));

        assertThat(sessionQueryHosting(hosts).screenOf(sessionId())).isEqualTo(screenSaying("hello"));
    }

    @Test
    void shouldTellAListenerWhenAHostedSessionDrew() {
        HostPort hosts = mock(HostPort.class);
        Runnable listener = mock(Runnable.class);
        ArgumentCaptor<Consumer<SessionId>> drew = ArgumentCaptor.captor();
        sessionQueryHosting(hosts).onScreenChange(listener);
        verify(hosts).onScreenChange(drew.capture());

        drew.getValue().accept(sessionId());

        verify(listener).run();
    }

    @Test
    void shouldViewTheVerbsAndTheOneToPreselect() {

        assertThat(sessionQuery(sessions, launchLog, watch, warnings)
                        .verbChoiceFor(verbChoicePayload(List.of(Verb.REVIEW, Verb.DESIGN), false)))
                .isEqualTo(VerbChoiceView.builder()
                        .verbs(List.of(Verb.REVIEW, Verb.DESIGN))
                        .preselected(Verb.DESIGN)
                        .build());
    }

    @Test
    void shouldHandTheListenerAViewOnRefresh() {
        List<SessionsView> received = new ArrayList<>();
        SessionQuery query = sessionQuery(sessions, launchLog, watch, warnings);
        query.subscribe(OF_THE_PROJECT, received::add);

        query.refresh(OF_THE_PROJECT);

        assertThat(received).containsExactly(emptySessionsView());
    }

    @Test
    void shouldHandTheListenerAViewWhenToldOfTheTicketsOnTheBoard() {
        List<SessionsView> received = new ArrayList<>();
        SessionQuery query = sessionQuery(sessions, launchLog, watch, warnings);
        query.subscribe(OF_THE_PROJECT, received::add);

        query.knowTickets(OF_THE_PROJECT);

        assertThat(received).containsExactly(emptySessionsView());
    }
}
