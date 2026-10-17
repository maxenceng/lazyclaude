package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.backlogMissing;
import static dev.maxenceng.lazyclaude.board.published.ReasonFixture.treeUnwatchable;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class BoardManagerWatchTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BranchCopyPort copies;

    @Mock
    private BranchChangesPort changes;

    @Mock
    private BoardWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Mock
    private Runnable changed;

    @InjectMocks
    private BoardManager manager;

    @Test
    void shouldRunTheListenerWhenTheWatchReportsAChange() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        manager.watch(workingDirectory(), changed);
        ArgumentCaptor<Runnable> reported = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), reported.capture(), any());

        reported.getValue().run();

        verify(changed).run();
    }

    @Test
    void shouldWatchTheProjectTheWorkingDirectoryBelongsTo() {
        when(locator.locate(workingDirectory())).thenReturn(project());

        manager.watch(workingDirectory(), changed);

        verify(watch).watch(any(), any(), any());
        verifyNoInteractions(warnings);
    }

    @Test
    void shouldRaiseAWarningNamingTheTreeAndHowOftenItIsCheckedWhenATreeIsPolled() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        manager.watch(workingDirectory(), changed);
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), any(), fallback.capture());

        fallback.getValue().polling(treeUnwatchable(), Duration.ofSeconds(5));

        assertThat(raisedTexts(warnings))
                .containsExactly(
                        "board: cannot watch /home/max/projects/app/docs/backlog (User limit of inotify watches reached; if the watch limit is reached, raise fs.inotify.max_user_watches); checking every 5 s");
    }

    @Test
    void shouldRaiseAWarningForEachTreeThatIsPolled() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        manager.watch(workingDirectory(), changed);
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), any(), fallback.capture());

        fallback.getValue().polling(treeUnwatchable(), Duration.ofSeconds(5));
        fallback.getValue().polling(treeUnwatchable(), Duration.ofSeconds(5));

        assertThat(raisedTexts(warnings)).as("the watcher says it once for each tree, so each is named").hasSize(2);
    }

    @Test
    void shouldRaiseAWarningSayingWhyWhenTheBoardCannotBeReadAgain() {
        manager.boardUnreadable(backlogMissing());

        assertThat(raisedTexts(warnings))
                .containsExactly("board: the main worktree has no /x/docs/backlog; showing the board as last read");
    }

    @Test
    void shouldNotRaiseTheSameWarningAgainWhileTheBoardKeepsFailingToBeRead() {
        manager.boardUnreadable(backlogMissing());
        manager.boardUnreadable(backlogMissing());

        assertThat(raisedTexts(warnings)).hasSize(1);
    }
}
