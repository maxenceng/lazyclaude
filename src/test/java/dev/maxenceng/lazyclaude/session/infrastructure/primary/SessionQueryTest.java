package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.onBranches;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedIn;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionActiveMinutesAgo;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.newestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.oldestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.verbChoicePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQueryHosting;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.newestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.oldestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.unavailableSessionsView;
import static dev.maxenceng.lazyclaude.session.published.HostedSessionIdFixture.hostedSessionId;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketIds;
import static dev.maxenceng.lazyclaude.session.published.TerminalScreenFixture.screenSaying;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
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
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Test
    void shouldViewNoSessionsWhenNoneIsLive() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(sessionQuery(sessions, locator, watch, warnings).sessionsFor(OF_THE_PROJECT)).isEqualTo(emptySessionsView());
    }

    @Test
    void shouldViewEachSessionWithItsAgeNewestFirst() {
        when(sessions.liveSessions(any())).thenReturn(List.of(oldestReportedSession(), reportedSession(), newestReportedSession()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionQuery(sessions, locator, watch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));
    }

    @Test
    void shouldViewWhyWhenNoSourceAnswers() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        assertThat(sessionQuery(sessions, locator, watch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(unavailableSessionsView());
    }

    @Test
    void shouldHandTheListenerAViewWheneverTheWatcherReportsAChange() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<SessionsView> received = new ArrayList<>();

        sessionQuery(sessions, locator, watch, warnings).subscribe(OF_THE_PROJECT, received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(changed.capture(), any());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received).containsExactly(emptySessionsView(), emptySessionsView());
    }

    @Test
    void shouldViewEachPastSessionWithTheTimeSinceItsLastActivityNewestFirst() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions())
                .thenReturn(List.of(
                        locatedIn(pastSessionActiveMinutesAgo(3, 30)),
                        locatedIn(pastSessionActiveMinutesAgo(1, 10))));

        assertThat(sessionQuery(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(pastSessionsViewOf(List.of(pastSessionViewNumbered(1), pastSessionViewNumbered(3))));
    }

    @Test
    void shouldViewThePastSessionsAfterTheLiveOnes() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(any())).thenReturn(project());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));

        assertThat(sessionQuery(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(sessionsViewOf(List.of(sessionView()), List.of(pastSessionView())));
    }

    @Test
    void shouldViewTheLinksOfALiveSessionSpeltAsTheBoardSpellsThem() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(any())).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(sessionView().id(), ran(ticketRun("start", "proj-1"))));

        assertThat(sessionQuery(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(WITH_TICKETS))
                .isEqualTo(sessionsViewOf(List.of(sessionViewBuilder().links(List.of(strong("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldViewTheLinksOfAPastSession() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        when(past.pastSessions()).thenReturn(List.of(locatedPastSession()));
        when(past.linkFacts()).thenReturn(Map.of(pastSessionView().id(), onBranches("PROJ-2")));

        assertThat(sessionQuery(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(WITH_TICKETS))
                .isEqualTo(pastSessionsViewOf(List.of(pastSessionViewBuilder().links(List.of(weak("PROJ-2"))).build())));
    }

    @Test
    void shouldViewNoLinkWhenThePayloadHandsOverNoTicket() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(any())).thenReturn(project());
        when(past.linkFacts()).thenReturn(Map.of(sessionView().id(), ran(ticketRun("start", "PROJ-1"))));

        assertThat(sessionQuery(sessions, past, locator, watch, transcriptWatch, warnings).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(sessionsViewOf(List.of(sessionView())));
    }

    @Test
    void shouldAnswerWithTheScreenTheHostHolds() {
        HostPort hosts = mock(HostPort.class);
        when(hosts.screenOf(hostedSessionId())).thenReturn(screenSaying("hello"));

        assertThat(sessionQueryHosting(hosts).screenOf(hostedSessionId())).isEqualTo(screenSaying("hello"));
    }

    @Test
    void shouldTellAListenerWhenAHostedSessionDrew() {
        HostPort hosts = mock(HostPort.class);
        Runnable listener = mock(Runnable.class);
        ArgumentCaptor<Consumer<HostedSessionId>> drew = ArgumentCaptor.captor();
        sessionQueryHosting(hosts).onScreenChange(listener);
        verify(hosts).onScreenChange(drew.capture());

        drew.getValue().accept(hostedSessionId());

        verify(listener).run();
    }

    @Test
    void shouldViewTheVerbsAndTheOneToPreselect() {
        when(sessions.liveSessions(any())).thenReturn(List.of());

        assertThat(sessionQuery(sessions, locator, watch, warnings)
                        .verbChoiceFor(verbChoicePayload(List.of(Verb.REVIEW, Verb.DESIGN), false)))
                .isEqualTo(VerbChoiceView.builder()
                        .verbs(List.of(Verb.REVIEW, Verb.DESIGN))
                        .preselected(Verb.DESIGN)
                        .build());
    }

    @Test
    void shouldHandTheListenerAViewOnRefresh() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<SessionsView> received = new ArrayList<>();
        SessionQuery query = sessionQuery(sessions, locator, watch, warnings);
        query.subscribe(OF_THE_PROJECT, received::add);

        query.refresh(OF_THE_PROJECT);

        assertThat(received).containsExactly(emptySessionsView());
    }

    @Test
    void shouldHandTheListenerAViewWhenToldOfTheTicketsOnTheBoard() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<SessionsView> received = new ArrayList<>();
        SessionQuery query = sessionQuery(sessions, locator, watch, warnings);
        query.subscribe(OF_THE_PROJECT, received::add);

        query.knowTickets(OF_THE_PROJECT);

        assertThat(received).containsExactly(emptySessionsView());
    }
}
