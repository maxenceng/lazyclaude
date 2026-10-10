package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.newestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.oldestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQueryFixture.sessionQuery;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.newestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.oldestSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionViewFixture.unavailableSessionsView;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;

@ExtendWith(MockitoExtension.class)
class SessionQueryTest {

    private static final SessionPayload OF_THE_PROJECT = new SessionPayload(project().root());

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Test
    void shouldViewNoSessionsWhenNoneIsLive() {
        when(sessions.liveSessions()).thenReturn(List.of());

        assertThat(sessionQuery(sessions, locator, watch).sessionsFor(OF_THE_PROJECT)).isEqualTo(emptySessionsView());
    }

    @Test
    void shouldViewEachSessionWithItsAgeNewestFirst() {
        when(sessions.liveSessions()).thenReturn(List.of(oldestReportedSession(), reportedSession(), newestReportedSession()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionQuery(sessions, locator, watch).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(sessionsViewOf(List.of(newestSessionView(), sessionView(), oldestSessionView())));
    }

    @Test
    void shouldViewWhyWhenNoSourceAnswers() {
        when(sessions.liveSessions()).thenThrow(sessionsUnavailable());

        assertThat(sessionQuery(sessions, locator, watch).sessionsFor(OF_THE_PROJECT))
                .isEqualTo(unavailableSessionsView());
    }

    @Test
    void shouldHandTheListenerAViewWheneverTheWatcherReportsAChange() {
        when(sessions.liveSessions()).thenReturn(List.of());
        List<SessionsView> received = new ArrayList<>();

        sessionQuery(sessions, locator, watch).subscribe(OF_THE_PROJECT, received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(changed.capture());
        changed.getValue().run();
        changed.getValue().run();

        assertThat(received).containsExactly(emptySessionsView(), emptySessionsView());
    }
}
