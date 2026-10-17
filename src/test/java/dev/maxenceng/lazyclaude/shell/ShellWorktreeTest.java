package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewIn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.aheadWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.clearHeldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.heldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.occupiedWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.drained;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.screensShowing;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnDoneTicket;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnDoneTicketWithWorktree;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnInReviewTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoMergedPullRequestView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RefusedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RemovedWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreePayload;
import dev.maxenceng.lazyclaude.session.published.Reason;

@ExtendWith(MockitoExtension.class)
class ShellWorktreeTest {

    private static final String PROMPT = "remove worktree ../app.worktrees/PROJ-1-ticket? y/n";

    @Mock
    private TicketWorktreeCommand removing;

    @Mock
    private HostedSessionCommand hosting;

    @Test
    void shouldShowTheRemoveKeyOnADoneTicketThatHasItsWorktree() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);

        assertThat(Screen.of(shell).lines().getLast()).contains("d remove worktree");
    }

    @Test
    void shouldShowNoRemoveKeyOnADoneTicketWithNoWorktree() {
        Shell shell = shellOnDoneTicketWithWorktree(new NoWorktreeView(), removing);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("remove worktree");
    }

    @Test
    void shouldShowNoRemoveKeyOnADoneTicketWhoseWorktreeCouldNotBeRead() {
        Shell shell = shellOnDoneTicketWithWorktree(new UnreadableWorktreeView(new Reason("git is gone")), removing);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("remove worktree");
    }

    @Test
    void shouldShowNoRemoveKeyOnATicketThatIsNotDoneEvenWithAWorktree() {
        Shell shell = shellOnInReviewTicket(clearHeldWorktreeView(), hosting);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("remove worktree");
    }

    @Test
    void shouldBindNothingToDOnATicketThatIsNotDone() {
        Shell shell = shellOnInReviewTicket(clearHeldWorktreeView(), hosting);

        shell.press(key('d'));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        verify(removing, never()).remove(any());
    }

    @Test
    void shouldShowNoRemoveKeyOutsideTheBoard() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('l'));

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("remove worktree");
    }

    @Test
    void shouldAskBeforeRemovingWhenEveryCheckPasses() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);

        shell.press(key('d'));

        assertThat(Screen.of(shell).statusLine()).contains(PROMPT);
        verify(removing, never()).remove(any());
    }

    @Test
    void shouldRunTheChecksOffTheRenderThread() {
        List<Runnable> queued = new ArrayList<>();
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, queued::add);
        drained(queued);

        shell.press(key('d'));

        assertThat(queued).isNotEmpty();
        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        drained(queued);
        assertThat(Screen.of(shell).statusLine()).contains(PROMPT);
    }

    @Test
    void shouldRemoveTheWorktreeOnY() {
        when(removing.remove(any())).thenReturn(new RemovedWorktreeView());
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));
        shell.press(key('d'));

        shell.press(key('y'));

        ArgumentCaptor<WorktreePayload> payload = ArgumentCaptor.forClass(WorktreePayload.class);
        verify(removing).remove(payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo("PROJ-1");
        assertThat(payload.getValue().mergedHead()).as("review's last read of the merge is what removal is judged by").isEqualTo(headCommit());
        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
    }

    @Test
    void shouldJudgeAgainstNoMergedHeadBeforeReviewHasReadAny() {
        when(removing.remove(any())).thenReturn(new RemovedWorktreeView());
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));

        shell.press(key('y'));

        ArgumentCaptor<WorktreePayload> payload = ArgumentCaptor.forClass(WorktreePayload.class);
        verify(removing).remove(payload.capture());
        assertThat(payload.getValue().mergedHead()).isEqualTo(new NoMergedHead());
    }

    @Test
    void shouldRemoveOffTheRenderThreadAndOnlyAfterTheChecks() {
        List<Runnable> queued = new ArrayList<>();
        when(removing.remove(any())).thenReturn(new RemovedWorktreeView());
        Shell shell = shellOnDoneTicket(screensShowing(clearHeldWorktreeView()), removing, queued::add);
        drained(queued);
        shell.press(key('d'));
        drained(queued);

        shell.press(key('y'));

        verify(removing, never()).remove(any());
        drained(queued);
        verify(removing).remove(any());
    }

    @Test
    void shouldLeaveEverythingAsItWasOnN() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));

        shell.press(key('n'));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        verify(removing, never()).remove(any());
    }

    @Test
    void shouldLeaveEverythingAsItWasOnEscape() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));

        shell.press(escape());

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        verify(removing, never()).remove(any());
    }

    @Test
    void shouldAskNothingAndNameTheDirtyPathsWhenSomeAreDirty() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(dirtyWorktreeContentsView(), false), removing);

        shell.press(key('d'));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        assertThat(Screen.of(shell).lines().getLast()).contains("cannot remove: 1 dirty path — src/Foo.java");
    }

    @Test
    void shouldNameTheCommitsBeyondTheMergeWhenThereAreSome() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(aheadWorktreeContentsView(), false), removing);

        shell.press(key('d'));

        assertThat(Screen.of(shell).lines().getLast()).contains("cannot remove: 1 commit beyond the merge that no remote has");
    }

    @Test
    void shouldSayItCannotTellWhatTheMergeCoveredWhenNoMergedPullRequestIsKnown() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(new NoMergedPullRequestView(), false), removing);

        shell.press(key('d'));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        assertThat(Screen.of(shell).lines().getLast()).contains("cannot tell what the merge covered");
    }

    @Test
    void shouldNameTheLiveSessionsInTheWorktree() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(occupiedWorktreeContentsView(), false), removing);

        shell.press(key('d'));

        assertThat(Screen.of(shell).lines().getLast())
                .contains("cannot remove: live session \"lazyclaude-b6\" is in the worktree");
    }

    @Test
    void shouldSayWhyWhenTheChecksCannotBeMade() {
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, Runnable::run);
        when(screens.worktreeOf(any())).thenReturn(new UnreadableWorktreeView(new Reason("git status: fatal")));
        shell.press(key('d'));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        assertThat(Screen.of(shell).lines().getLast()).contains("cannot remove: git status: fatal");
    }

    @Test
    void shouldShowGitsMessageAndAskNothingMoreWhenGitFailsToRemove() {
        when(removing.remove(any()))
                .thenReturn(new FailedRemovalView(new Reason("git worktree remove: fatal: it is locked")));
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));

        shell.press(key('y'));

        assertThat(Screen.of(shell).lines().getLast())
                .contains("cannot remove: git worktree remove: fatal: it is locked");
        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        assertThat(Screen.of(shell).boardPanelText()).as("the screen is left as it was").contains("PROJ-1");
    }

    @Test
    void shouldShowWhatStoodInTheWayWhenTheStateChangedBetweenTheQuestionAndTheYes() {
        when(removing.remove(any())).thenReturn(new RefusedRemovalView(dirtyWorktreeContentsView()));
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);
        shell.press(key('d'));

        shell.press(key('y'));

        assertThat(Screen.of(shell).lines().getLast()).contains("cannot remove: 1 dirty path — src/Foo.java");
    }

    @Test
    void shouldClearTheRefusalOnTheNextKey() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(dirtyWorktreeContentsView(), false), removing);
        shell.press(key('d'));

        shell.press(key('k'));

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("cannot remove");
    }

    @Test
    void shouldStopOfferingTheKeyOnceTheWorktreeIsRemoved() {
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        when(removing.remove(any())).thenReturn(new RemovedWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, Runnable::run);
        shell.press(key('d'));
        when(screens.worktreeOf(any())).thenReturn(new NoWorktreeView());

        shell.press(key('y'));

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("remove worktree");
    }

    @Test
    void shouldAskWhenAHostedDoneSessionEndsAndTheTicketReadsDone() {
        BoardQuery boards = mock(BoardQuery.class);
        when(boards.boardReadAgainFor(any())).thenReturn(Optional.of(boardViewOf(doneTicketView())));
        Shell shell = shellOnDoneTicket(removing, boards, clearHeldWorktreeView());

        shell.ticketDoneEnded(linkedTicketId());

        assertThat(Screen.of(shell).statusLine()).contains(PROMPT);
    }

    @Test
    void shouldAskNothingWhenTheTicketDoesNotReadDoneAfterItsDoneSessionEnds() {
        BoardQuery boards = mock(BoardQuery.class);
        when(boards.boardReadAgainFor(any())).thenReturn(Optional.of(boardViewOf(ticketViewIn(Status.IN_REVIEW))));
        Shell shell = shellOnDoneTicket(removing, boards, clearHeldWorktreeView());

        shell.ticketDoneEnded(linkedTicketId());

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
    }

    @Test
    void shouldAskNothingWhenTheDoneTicketHasNoWorktree() {
        BoardQuery boards = mock(BoardQuery.class);
        when(boards.boardReadAgainFor(any())).thenReturn(Optional.of(boardViewOf(doneTicketView())));
        Shell shell = shellOnDoneTicket(removing, boards, new NoWorktreeView());

        shell.ticketDoneEnded(linkedTicketId());

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
    }

    @Test
    void shouldRefuseWithTheReasonWhenTheDoneSessionEndsOverADirtyWorktree() {
        BoardQuery boards = mock(BoardQuery.class);
        when(boards.boardReadAgainFor(any())).thenReturn(Optional.of(boardViewOf(doneTicketView())));
        Shell shell = shellOnDoneTicket(removing, boards, heldWorktreeView(dirtyWorktreeContentsView(), false));

        shell.ticketDoneEnded(linkedTicketId());

        assertThat(Screen.of(shell).statusLine()).doesNotContain("remove worktree");
        assertThat(Screen.of(shell).lines().getLast()).contains("cannot remove: 1 dirty path — src/Foo.java");
    }

    @Test
    void shouldAskForTheEndedTicketWhateverIsSelectedWhenItsSessionEnds() {
        BoardQuery boards = mock(BoardQuery.class);
        when(boards.boardReadAgainFor(any())).thenReturn(Optional.of(boardViewOf(doneTicketView())));
        Shell shell = shellOnDoneTicket(removing, boards, clearHeldWorktreeView());
        shell.press(key('k'));

        shell.ticketDoneEnded(linkedTicketId());

        assertThat(Screen.of(shell).statusLine()).contains(PROMPT);
    }

    @Test
    void shouldDropACheckThatLandsAfterAnotherRowWasSelected() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellOnDoneTicket(screensShowing(clearHeldWorktreeView()), removing, queued::add);
        drained(queued);
        shell.press(key('d'));
        shell.press(key('k'));

        drained(queued);

        assertThat(Screen.of(shell).statusLine())
                .as("the answer is about a ticket no longer selected")
                .doesNotContain("remove worktree");
    }

    @Test
    void shouldShowTheWorktreePathAndThatItIsCleanInTheTicketDetail() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);

        assertThat(Screen.of(shell).text()).contains("worktree  ../app.worktrees/PROJ-1-ticket  clean");
    }

    @Test
    void shouldShowTheNumberOfDirtyPathsInTheTicketDetail() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(dirtyWorktreeContentsView(), false), removing);

        assertThat(Screen.of(shell).text()).contains("worktree  ../app.worktrees/PROJ-1-ticket").contains("1 dirty path");
    }

    @Test
    void shouldShowTheNumberOfCommitsBeyondTheMergeInTheTicketDetail() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(aheadWorktreeContentsView(), false), removing);

        assertThat(Screen.of(shell).text()).contains("1 commit beyond the merge");
    }

    @Test
    void shouldShowThatNoMergedPullRequestIsKnownInTheTicketDetail() {
        Shell shell = shellOnDoneTicketWithWorktree(heldWorktreeView(new NoMergedPullRequestView(), false), removing);

        assertThat(Screen.of(shell).text()).contains("no merged pull request");
    }

    @Test
    void shouldShowNoWorktreeRowForATicketWithNoWorktree() {
        Shell shell = shellOnDoneTicketWithWorktree(new NoWorktreeView(), removing);

        assertThat(Screen.of(shell).text()).doesNotContain("worktree  ");
    }

    @Test
    void shouldShowNothingOfTheWorktreeOnTheBoard() {
        Shell shell = shellOnDoneTicketWithWorktree(clearHeldWorktreeView(), removing);

        assertThat(Screen.of(shell).boardPanelText()).doesNotContain("worktree").doesNotContain("clean");
    }

    @Test
    void shouldReadTheWorktreeAgainWhenTheSessionsChange() {
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, Runnable::run);
        Screen.of(shell);

        shell.sessionsChanged(emptySessionsView());
        Screen.of(shell);

        verify(screens, times(2)).worktreeOf(any());
    }

    @Test
    void shouldReadTheWorktreeOnceWhileNothingChanges() {
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, Runnable::run);

        Screen.of(shell);
        Screen.of(shell);

        verify(screens, times(1)).worktreeOf(any());
    }

    @Test
    void shouldNotReadTheWorktreeAgainOnceTheSelectionLeavesTheTicket() {
        SessionQuery screens = screensShowing(clearHeldWorktreeView());
        Shell shell = shellOnDoneTicket(screens, removing, Runnable::run);
        shell.press(key('k'));

        Screen.of(shell);

        verify(screens, times(1)).worktreeOf(any());
    }
}
