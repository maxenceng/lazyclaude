package dev.maxenceng.lazyclaude.session.application;

import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.locatedPastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.clock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
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
import dev.maxenceng.lazyclaude.session.domain.PastSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionWatchPort;
import dev.maxenceng.lazyclaude.session.domain.Sessions;
import dev.maxenceng.lazyclaude.session.domain.TranscriptWatchPort;
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

    @Test
    void shouldReadTheLiveSessionsOfTheProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession())))
                .thenReturn(project());

        assertThat(new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, clock()).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldHandTheListenerWhyAndKeepWatchingWhenARereadCannotAnswer() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable()).thenReturn(List.of());
        List<Sessions> received = new ArrayList<>();

        new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, clock()).subscribe(project(), received::add);
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

        assertThat(new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, clock()).sessionsFor(project()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldHandTheListenerTheSessionsAgainWhenTheTranscriptWatcherReportsAChange() {
        when(sessions.liveSessions(any())).thenReturn(List.of());
        List<Sessions> received = new ArrayList<>();

        new SessionApplicationService(sessions, past, locator, watch, transcriptWatch, warnings, clock()).subscribe(project(), received::add);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), changed.capture(), any(), any(), any());
        changed.getValue().run();

        assertThat(received)
                .as("the transcripts are read on their own thread, so their landing must reach the panel without waiting for the registry")
                .containsExactly(liveSessionsOf(List.of()));
    }
}
