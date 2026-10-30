package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.answeredWith;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.answeredWithoutAnEtag;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.everyFewMillis;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.failed;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.firstEtag;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.notModified;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.secondEtag;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchFixture.seconds;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;

class IssuesWatchRepositoryTest {

    private final GhCommand gh = mock(GhCommand.class);
    private IssuesWatchRepository watcher = new IssuesWatchRepository(gh, Duration.ofSeconds(30));

    @AfterEach
    void stopWatching() {
        watcher.close();
    }

    @Test
    void shouldCheckEveryThirtySecondsByDefault() {
        assertThat(IssuesWatchRepository.INTERVAL).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldOnlyTakeTheEtagOnTheFirstCheckSinceTheBoardWasJustRead() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag()));

        assertThat(watcher.changedSinceLastCheck(project())).isFalse();
    }

    @Test
    void shouldAskWithoutAConditionOnTheFirstCheck() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag()));

        watcher.changedSinceLastCheck(project());

        verify(gh).run(project(), List.of("api", "--include", "--silent", "repos/{owner}/{repo}/issues?state=all&per_page=1&sort=updated&direction=desc"));
    }

    @Test
    void shouldAskConditionallyOnTheEtagOfTheLastAnswer() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag()));
        watcher.changedSinceLastCheck(project());

        watcher.changedSinceLastCheck(project());

        verify(gh).run(
                project(),
                List.of(
                        "api", "--include", "--silent", "repos/{owner}/{repo}/issues?state=all&per_page=1&sort=updated&direction=desc",
                        "--header", "If-None-Match: W/\"a\""));
    }

    @Test
    void shouldReportNoChangeWhenGitHubAnswersNotModified() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(notModified());
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).as("a 304 makes gh exit non-zero, and is still no change").isFalse();
    }

    @Test
    void shouldReportAChangeWhenTheEtagDiffers() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(answeredWith(secondEtag()));
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).isTrue();
    }

    @Test
    void shouldReportNoChangeWhenTheEtagIsTheSame() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag()));
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).isFalse();
    }

    @Test
    void shouldCompareAgainstTheEtagOfTheLastChangeNotTheFirst() {
        when(gh.run(eq(project()), any()))
                .thenReturn(answeredWith(firstEtag()))
                .thenReturn(answeredWith(secondEtag()))
                .thenReturn(answeredWith(secondEtag()));
        watcher.changedSinceLastCheck(project());
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).isFalse();
    }

    @Test
    void shouldReportThatItCannotTellWhenTheCheckFails() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(failed());
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).as("so the board is read, and says why it cannot be").isTrue();
    }

    @Test
    void shouldReportThatItCannotTellWhenGhCannotBeRun() {
        when(gh.run(eq(project()), any())).thenThrow(new BacklogUnavailableException("cannot run gh: nope"));

        assertThat(watcher.changedSinceLastCheck(project())).isTrue();
    }

    @Test
    void shouldReportAChangeOnTheFirstGoodCheckAfterAFailureSinceTheBoardMayHaveBeenKeptStale() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(failed()).thenReturn(answeredWith(firstEtag()));
        watcher.changedSinceLastCheck(project());
        watcher.changedSinceLastCheck(project());

        assertThat(watcher.changedSinceLastCheck(project())).isTrue();
    }

    @Test
    void shouldAskWithoutAConditionAfterAFailure() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(failed()).thenReturn(answeredWith(firstEtag()));
        watcher.changedSinceLastCheck(project());
        watcher.changedSinceLastCheck(project());

        watcher.changedSinceLastCheck(project());

        verify(gh, times(2))
                .run(project(), List.of("api", "--include", "--silent", "repos/{owner}/{repo}/issues?state=all&per_page=1&sort=updated&direction=desc"));
    }

    @Test
    void shouldReportThatItCannotTellWhenTheFirstCheckFails() {
        when(gh.run(eq(project()), any())).thenReturn(failed());

        assertThat(watcher.changedSinceLastCheck(project())).isTrue();
    }

    @Test
    void shouldReportThatItCannotTellWhenTheAnswerCarriesNoEtag() {
        when(gh.run(eq(project()), any())).thenReturn(answeredWithoutAnEtag());

        assertThat(watcher.changedSinceLastCheck(project())).isTrue();
    }

    @Test
    void shouldRunTheListenerOnADaemonThreadOfItsOwnWhenTheIssuesChange() throws InterruptedException {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(answeredWith(secondEtag()));
        watcher = new IssuesWatchRepository(gh, everyFewMillis());
        CountDownLatch changed = new CountDownLatch(1);
        AtomicReference<Thread> ranOn = new AtomicReference<>();

        watcher.watch(project(), () -> {
            ranOn.set(Thread.currentThread());
            changed.countDown();
        });

        assertThat(changed.await(seconds().toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(ranOn.get()).isNotSameAs(Thread.currentThread());
        assertThat(ranOn.get().isDaemon()).as("a daemon, so the watch never keeps lazyclaude alive").isTrue();
    }

    @Test
    void shouldKeepCheckingEveryIntervalWhileNothingChanges() throws InterruptedException {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag())).thenReturn(notModified());
        watcher = new IssuesWatchRepository(gh, everyFewMillis());
        Runnable listener = mock(Runnable.class);

        watcher.watch(project(), listener);

        verify(gh, timeout(seconds().toMillis()).atLeast(3)).run(eq(project()), any());
        verifyNoInteractions(listener);
    }

    @Test
    void shouldStopCheckingWhenClosed() throws InterruptedException {
        when(gh.run(eq(project()), any())).thenReturn(answeredWith(firstEtag()));
        watcher = new IssuesWatchRepository(gh, everyFewMillis());
        watcher.watch(project(), () -> {});
        verify(gh, timeout(seconds().toMillis()).atLeast(2)).run(eq(project()), any());

        watcher.close();
        Thread.sleep(100);
        clearInvocations(gh);
        Thread.sleep(100);

        verifyNoInteractions(gh);
    }
}
