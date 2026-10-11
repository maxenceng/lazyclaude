package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.oldestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionIn;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
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
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionManager;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionsUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.workingDirectoryOf;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnreadable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.otherProjectDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.outsideGitDirectory;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.Busy;
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
    private WarningPort warnings;

    @Test
    void shouldListASessionWhoseDirectoryBelongsToTheProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldMeasureEveryAgeOfOneReadFromTheSameInstant() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        LiveSessions read = (LiveSessions) new SessionManager(sessions, locator, watch, warnings, new SessionClock(advancingClock())).sessionsFor(project());

        assertThat(read.ageOf(liveSession()))
                .as("rows of one read drawn at different instants would disagree on how long ago they started")
                .isEqualTo(read.ageOf(liveSession()));
    }

    @Test
    void shouldListASessionInATicketWorktreeBecauseTheLocatorPointsItAtTheMainWorktree() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(worktreeDirectory())));
        when(locator.locate(worktreeDirectory())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(sessionIn(worktreeDirectory()))));
    }

    @Test
    void shouldNotListASessionOfAnotherProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(otherProjectDirectory())));
        when(locator.locate(otherProjectDirectory())).thenReturn(new Project(Path.of("/work/other")));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of()));
    }

    @Test
    void shouldNotListASessionWhoseDirectoryBelongsToNoProject() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedIn(outsideGitDirectory()), reportedSession()));
        when(locator.locate(outsideGitDirectory())).thenThrow(new ProjectNotFoundException("not inside a git repository"));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldSayWhyWhenTheProjectOfASessionCannotBeLocated() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession())))
                .thenThrow(new ProjectUnavailableException(reason().value(), new InterruptedException()));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .as("a git that failed says nothing about where the session is, so the panel must not claim there is none")
                .isEqualTo(unavailableSessions());
    }

    @Test
    void shouldLocateEachDistinctDirectoryOnce() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedSession(), newestReportedSession(), oldestReportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        sessionManager(sessions, locator, watch, warnings).sessionsFor(project());

        verify(locator, times(1)).locate(workingDirectoryOf(reportedSession()));
    }

    @Test
    void shouldNotListAnSdkCliSession() {
        when(sessions.liveSessions(any())).thenReturn(List.of(sdkCliSession(), reportedSession()));
        when(locator.locate(workingDirectoryOf(reportedSession()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project())).isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListASessionThatReportsNoEntrypoint() {
        when(sessions.liveSessions(any())).thenReturn(List.of(sessionWithoutEntrypoint()));
        when(locator.locate(workingDirectoryOf(sessionWithoutEntrypoint()))).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .as("claude agents --json has no entrypoint, so an sdk-cli session cannot be told apart there")
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldOrderNewestStartFirst() {
        when(sessions.liveSessions(any())).thenReturn(List.of(oldestReportedSession(), newestReportedSession(), reportedSession()));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), liveSession(), oldestSession())));
    }

    @Test
    void shouldKeepTheOrderWhenAStateChanges() {
        when(locator.locate(any())).thenReturn(project());
        when(sessions.liveSessions(any()))
                .thenReturn(List.of(reportedWith(ReportedStatus.BUSY), newestReportedSession()))
                .thenReturn(List.of(reportedWith(ReportedStatus.IDLE), newestReportedSession()))
                .thenReturn(List.of(reportedWaitingFor(inputNeeded()), newestReportedSession()));

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new Busy()))));
        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new YourTurn()))));
        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(newestSession(), sessionWith(new WaitingForInput(inputNeeded())))));
    }

    @Test
    void shouldBeBusyWhenTheStatusIsShell() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWith(ReportedStatus.SHELL)));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Busy()))));
    }

    @Test
    void shouldWaitForPermissionOnAPermissionPrompt() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWaitingFor(permissionPrompt())));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new WaitingForPermission()))));
    }

    @Test
    void shouldWaitForInputWhenNoReasonIsGiven() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWaitingFor(new NoWaitingReason())));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
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

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .as("only a waiting session is blocked, whatever stale reason its record carries")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new YourTurn()))));
    }

    @Test
    void shouldListASessionWhoseStatusItDoesNotKnowAsUnknown() {
        when(sessions.liveSessions(any())).thenReturn(List.of(reportedWith(ReportedStatus.UNKNOWN)));
        when(locator.locate(any())).thenReturn(project());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
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

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .as("a session of another project has no say in whether this project's sessions can be shown")
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldSayWhyWhenNoSourceAnswers() {
        when(sessions.liveSessions(any())).thenThrow(sessionsUnavailable());

        assertThat(sessionManager(sessions, locator, watch, warnings).sessionsFor(project()))
                .as("unavailable sessions are shown as such, not a failed start")
                .isEqualTo(unavailableSessions());
    }

    @Test
    void shouldAskTheWatcherToTellItWhenSessionsMayHaveChanged() {
        Runnable changed = () -> {};

        sessionManager(sessions, locator, watch, warnings).watch(changed);

        verify(watch).watch(eq(changed), any());
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheWatchFallsBackToTheSweep() {
        sessionManager(sessions, locator, watch, warnings).watch(() -> {});
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
        sessionManager(sessions, locator, watch, warnings).sessionsFor(project());
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
        manager.sessionsFor(project());
        manager.sessionsFor(project());
        manager.watch(() -> {});
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

        sessionManager(sessions, locator, watch, warnings).sessionsFor(project());
        sessionManager(sessions, locator, watch, warnings).watch(() -> {});

        verifyNoInteractions(warnings);
    }
}
