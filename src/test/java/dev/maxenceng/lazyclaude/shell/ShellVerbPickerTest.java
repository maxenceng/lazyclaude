package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.inProgressDesignTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewIn;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.noMergedPullRequestsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceViewFixture.designPreselected;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.screensChoosing;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicket;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.startedTicketPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

@ExtendWith(MockitoExtension.class)
class ShellVerbPickerTest {

    private static final String IN_REVIEW_TITLE = "╭ PROJ-1 · in-review";

    @Mock
    private HostedSessionCommand hosting;

    @Test
    void shouldOpenTheVerbPickerOverTheSessionsPanelWithNOnATicket() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);

        shell.press(key('n'));

        assertThat(Screen.of(shell).text())
                .contains("╭ PROJ-1 · in-review ── checking PR… ╮")
                .contains("│ › respond")
                .contains("│   done")
                .contains("╰ enter start · esc cancel");
    }

    @Test
    void shouldListOnlyTheVerbsTheTicketsStatusAllows() {
        Shell shell = shellOnTicket(ticketViewIn(Status.TODO), hosting);

        shell.press(key('n'));

        assertThat(Screen.of(shell).text())
                .contains("│ › start")
                .doesNotContain("refine")
                .doesNotContain("respond");
    }

    @Test
    void shouldListDesignAfterReviewOnAnInProgressTicketWhoseBranchTouchesTheFrontend() {
        Shell shell = shellOnTicket(inProgressDesignTicketView(), emptySessionsView(), hosting, screensChoosing(designPreselected()));

        shell.press(key('n'));

        assertThat(Screen.of(shell).text()).containsSubsequence("│   review", "│ › design");
    }

    @Test
    void shouldNotReadCheckingForATicketNotInReview() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_PROGRESS), hosting);

        shell.press(key('n'));

        assertThat(Screen.of(shell).text()).contains("╭ PROJ-1 · in-progress ─").doesNotContain("checking PR");
    }

    @Test
    void shouldMoveTheCursorWithJAndK() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.press(key('j'));
        assertThat(Screen.of(shell).text()).contains("│   respond").contains("│ › done");
        shell.press(key('k'));

        assertThat(Screen.of(shell).text()).contains("│ › respond").contains("│   done");
    }

    @Test
    void shouldCancelWithEscapeWithoutStartingAnything() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.press(escape());

        assertThat(Screen.of(shell).text()).doesNotContain(IN_REVIEW_TITLE);
        verify(hosting, never()).startTicket(any());
    }

    @Test
    void shouldStartTheSelectedVerbOnTheTicketWithEnter() {
        when(hosting.startTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));
        shell.press(key('j'));

        shell.press(enter());

        TicketHostedPayload started = startedTicketPayload(hosting);
        assertThat(started.ticket()).isEqualTo(linkedTicketId());
        assertThat(started.branch()).isEqualTo(ticketBranch());
        assertThat(started.verb()).isEqualTo(Verb.DONE);
    }

    @Test
    void shouldCloseThePickerAndFocusTheNewTabWhenTheSessionStarts() {
        when(hosting.startTicket(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.press(enter());

        assertThat(Screen.of(shell).text()).doesNotContain(IN_REVIEW_TITLE);
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldKeepFocusAndClosePickerWhenNoSessionStarts() {
        when(hosting.startTicket(any())).thenReturn(new NotStartedHost());
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.press(enter());

        assertThat(Screen.of(shell).text()).doesNotContain(IN_REVIEW_TITLE);
        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldPreselectDoneWhenTheMergedReadLandsBeforeTheCursorMoved() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        assertThat(Screen.of(shell).text()).contains("│ › done").doesNotContain("checking PR");
    }

    @Test
    void shouldKeepTheCursorWhereItIsWhenTheMergedReadLandsAfterItMoved() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));
        shell.press(key('j'));
        shell.press(key('k'));

        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        assertThat(Screen.of(shell).text()).contains("│ › respond").doesNotContain("checking PR");
    }

    @Test
    void shouldStopCheckingWhenTheMergedReadLandsWithNoMergedPullRequest() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.mergedPullRequestsChanged(noMergedPullRequestsView());

        assertThat(Screen.of(shell).text()).contains("│ › respond").doesNotContain("checking PR");
    }

    @Test
    void shouldPreselectDoneAtOnceWhenTheMergedReadLandedBeforeThePickerOpened() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.mergedPullRequestsChanged(mergedPullRequestsViewOn("PROJ-1"));

        shell.press(key('n'));

        assertThat(Screen.of(shell).text()).contains("│ › done").doesNotContain("checking PR");
    }

    @Test
    void shouldStartAPlainSessionInTheProjectRootWithNOnADoneTicket() {
        when(hosting.startUnlinked(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOnTicket(doneTicketView(), emptySessionsView(), hosting, blankScreens());

        shell.press(key('n'));

        verify(hosting).startUnlinked(any());
        assertThat(Screen.of(shell).text()).doesNotContain("╭ PROJ-1 ·");
    }

    @Test
    void shouldDoNothingWithNWhenNoTicketIsSelected() {
        Shell shell = shellOnTicket(ticketView(), hosting);
        shell.press(key('k'));

        assertThat(shell.press(key('n'))).isFalse();
        verify(hosting, never()).startTicket(any());
    }

    @Test
    void shouldSwallowOtherKeysWhileThePickerIsOpen() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(key('n'));

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isFalse();
        assertThat(Screen.of(shell).text()).contains(IN_REVIEW_TITLE);
    }
}
