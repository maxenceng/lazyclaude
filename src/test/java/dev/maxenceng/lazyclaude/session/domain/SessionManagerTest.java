package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.oldestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sdkCliSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWithoutEntrypoint;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.advancingClock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionManager;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.published.SessionDirectoryFixture.otherProjectDirectory;
import static dev.maxenceng.lazyclaude.session.published.SessionDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.session.published.SessionDirectoryFixture.worktreeDirectory;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private SessionWatchPort watch;

    @Test
    void shouldListASessionWhoseDirectoryBelongsToTheProject() {
        when(sessions.liveSessions()).thenReturn(List.of(liveSession()));
        when(locator.locate(workingDirectoryOf(liveSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldMeasureEveryAgeOfOneReadFromTheSameInstant() {
        when(sessions.liveSessions()).thenReturn(List.of(liveSession()));
        when(locator.locate(workingDirectoryOf(liveSession()))).thenReturn(project());

        LiveSessions read = (LiveSessions) new SessionManager(sessions, locator, watch, new SessionClock(advancingClock())).sessionsFor(project());

        assertThat(read.ageOf(liveSession()))
                .as("rows of one read drawn at different instants would disagree on how long ago they started")
                .isEqualTo(read.ageOf(liveSession()));
    }

    @Test
    void shouldListASessionInATicketWorktreeBecauseTheLocatorPointsItAtTheMainWorktree() {
        LiveSession inWorktree = sessionIn(worktreeDirectory());
        when(sessions.liveSessions()).thenReturn(List.of(inWorktree));
        when(locator.locate(workingDirectoryOf(inWorktree))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(inWorktree)));
    }

    @Test
    void shouldNotListASessionOfAnotherProject() {
        LiveSession elsewhere = sessionIn(otherProjectDirectory());
        when(sessions.liveSessions()).thenReturn(List.of(elsewhere));
        when(locator.locate(new WorkingDirectory(otherProjectDirectory().value())))
                .thenReturn(new Project(Path.of("/work/other")));

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldNotListASessionWhoseDirectoryBelongsToNoProject() {
        LiveSession outside = sessionIn(outsideGitDirectory());
        when(sessions.liveSessions()).thenReturn(List.of(outside, liveSession()));
        when(locator.locate(new WorkingDirectory(outsideGitDirectory().value())))
                .thenThrow(new ProjectNotFoundException("not inside a git repository"));
        when(locator.locate(workingDirectoryOf(liveSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldLocateEachDistinctDirectoryOnce() {
        when(sessions.liveSessions()).thenReturn(List.of(liveSession(), newestSession(), oldestSession()));
        when(locator.locate(workingDirectoryOf(liveSession()))).thenReturn(project());

        sessionManager(sessions, locator, watch).sessionsFor(project());

        verify(locator, times(1)).locate(workingDirectoryOf(liveSession()));
    }

    @Test
    void shouldNotListAnSdkCliSession() {
        when(sessions.liveSessions()).thenReturn(List.of(sdkCliSession(), liveSession()));
        when(locator.locate(workingDirectoryOf(liveSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListASessionThatReportsNoEntrypoint() {
        when(sessions.liveSessions()).thenReturn(List.of(sessionWithoutEntrypoint()));
        when(locator.locate(workingDirectoryOf(sessionWithoutEntrypoint()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project()))
                .as("claude agents --json has no entrypoint, so an sdk-cli session cannot be told apart there")
                .isEqualTo(liveSessionsOf(List.of(sessionWithoutEntrypoint())));
    }

    @Test
    void shouldOrderNewestStartFirst() {
        when(sessions.liveSessions()).thenReturn(List.of(oldestSession(), newestSession(), liveSession()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), liveSession(), oldestSession())));
    }

    @Test
    void shouldKeepTheOrderWhenAStateChanges() {
        when(locator.locate(any())).thenReturn(project());
        when(sessions.liveSessions())
                .thenReturn(List.of(sessionWith(new Busy()), newestSession()))
                .thenReturn(List.of(sessionWith(new YourTurn()), newestSession()))
                .thenReturn(List.of(sessionWith(waitingForInput()), newestSession()));

        List<LiveSession> orders = List.of(newestSession(), sessionWith(new Busy()));
        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project())).isEqualTo(liveSessionsOf(orders));
        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new YourTurn()))));
        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(waitingForInput()))));
    }

    @Test
    void shouldSayWhyWhenNoSourceAnswers() {
        when(sessions.liveSessions()).thenThrow(sessionsUnavailable());

        assertThat(sessionManager(sessions, locator, watch).sessionsFor(project()))
                .as("unavailable sessions are shown as such, not a failed start")
                .isEqualTo(unavailableSessions());
    }

    @Test
    void shouldAskTheWatcherToTellItWhenSessionsMayHaveChanged() {
        Runnable changed = () -> {};

        sessionManager(sessions, locator, watch).watch(changed);

        verify(watch).watch(changed);
    }
}
