package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.inReviewSlugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.readThreadsViewOf;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffViewFixture.twoFileDiffView;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellReviewing;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellSubmitting;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.writingThatWrites;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingSubmitPayload;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.tamboui.tui.event.KeyCode;

class ShellReviewSubmitTest {

    @Test
    void shouldOpenTheSummaryInAPopupBoxTitledSummaryHoldingTheCurrentSummaryOnS() {
        Shell shell = shellSubmitting(inReviewSlugTicketView(), mock(PendingReviewCommand.class), mock(HostedSessionCommand.class));

        shell.press(key('S'));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("╭ Summary", "^s save  esc cancel", "Looks close. Two nits.", "The second is about naming.");
        assertThat(screen.cursor()).isPresent();
    }

    @Test
    void shouldAskWhetherToSubmitStraightAfterCtrlSWithoutWritingTheSummaryAnywhere() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        typed(shell, "!");

        shell.press(ctrl('s'));

        assertThat(Screen.of(shell).statusLine()).contains("submit review as Comment? y/n");
        assertThat(Screen.of(shell).text()).doesNotContain("^s save");
        verifyNoInteractions(writing);
    }

    @Test
    void shouldSendTheTypedSummaryAsTheBodyOfTheSubmit() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        typed(shell, "!");
        shell.press(ctrl('s'));

        shell.press(key('y'));

        verify(writing).submit(new PendingSubmitPayload(
                project().root(), pullRequestNumber(), new Summary("Looks close. Two nits.\nThe second is about naming.!")));
    }

    @Test
    void shouldKeepTheTypedSummaryForTheNextSWhileTheReviewIsOpenAndLoseItWhenTheReviewIsLeft() {
        Shell shell = shellReviewing(twoFileDiffView(), readThreadsViewOf(), noPendingReviewView());
        shell.press(key('S'));
        typed(shell, "mine");
        shell.press(ctrl('s'));
        shell.press(key('n'));

        shell.press(key('S'));
        assertThat(Screen.of(shell).text()).contains("│ mine");
        shell.press(namedKey(KeyCode.ESCAPE));
        shell.press(namedKey(KeyCode.ESCAPE));
        shell.press(key('p'));
        shell.press(key('S'));

        assertThat(Screen.of(shell).text()).doesNotContain("mine");
    }

    @Test
    void shouldShowTheTypedSummarysFirstLineInTheReviewPanelInsteadOfTheAdoptedBodyUntilItIsSubmitted() {
        Shell shell = shellSubmitting(inReviewSlugTicketView(), mock(PendingReviewCommand.class), mock(HostedSessionCommand.class));
        assertThat(Screen.of(shell).boardPanelLines()).contains("Looks close. Two nits.");
        shell.press(key('S'));
        ShellFixture.pressed(shell, namedKey(KeyCode.BACKSPACE), "The second is about naming.".length());
        ShellFixture.pressed(shell, namedKey(KeyCode.BACKSPACE), 1);
        shell.press(namedKey(KeyCode.HOME));
        ShellFixture.pressed(shell, namedKey(KeyCode.DELETE), "Looks close. Two nits.".length());
        typed(shell, "Rewritten");
        shell.press(ctrl('s'));
        shell.press(key('n'));

        assertThat(Screen.of(shell).boardPanelLines()).contains("Rewritten").doesNotContain("Looks close. Two nits.");
    }

    @Test
    void shouldSubmitOnYAndOfferToStartRespondInANewTabWhenTheTicketsStatusAllowsIt() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('y'));
        Screen screen = Screen.of(shell);

        verify(writing).submit(any());
        assertThat(screen.text()).contains("start /ticket respond PROJ-1?", "y start  esc back");
    }

    @Test
    void shouldStartRespondInTheTicketsWorktreeOnYAndCloseTheReview() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class, ShellFixture::noHostStarted);
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writingThatWrites(writtenView(noPendingReviewView())), hosting);
        shell.press(key('S'));
        shell.press(ctrl('s'));
        shell.press(key('y'));

        shell.press(key('y'));

        ArgumentCaptor<TicketHostedPayload> payload = ArgumentCaptor.forClass(TicketHostedPayload.class);
        verify(hosting).startTicket(payload.capture());
        assertThat(payload.getValue().verb()).isEqualTo(Verb.RESPOND);
        assertThat(payload.getValue().branch()).isEqualTo(inReviewSlugTicketView().branch().value());
        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("╭Files", "start /ticket respond");
    }

    @Test
    void shouldCloseTheReviewAndSelectTheTicketOnEscapeAtTheRespondDialogStartingNothing() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writingThatWrites(writtenView(noPendingReviewView())), hosting);
        shell.press(key('S'));
        shell.press(ctrl('s'));
        shell.press(key('y'));

        shell.press(namedKey(KeyCode.ESCAPE));
        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("╭Board").doesNotContain("╭Files", "start /ticket respond");
        assertThat(screen.reversedText()).anySatisfy(row -> assertThat(row).contains("PROJ-1"));
        verifyNoInteractions(hosting);
    }

    @Test
    void shouldCloseTheReviewWithoutADialogWhenTheTicketsStatusDoesNotAllowRespond() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);
        Shell shell = shellSubmitting(slugTicketView(), writingThatWrites(writtenView(noPendingReviewView())), hosting);
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('y'));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("start /ticket respond");
        verifyNoInteractions(hosting);
    }

    @Test
    void shouldSayNotSubmittedWithTheReasonAndKeepTheReviewPendingWhenGitHubRefusesTheSubmit() {
        PendingReviewCommand writing = mock(PendingReviewCommand.class);
        when(writing.submit(any())).thenReturn(refusedWriteView());
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('y'));
        Screen screen = Screen.of(shell);

        assertThat(screen.lines().getLast()).contains("not submitted (gh: not logged in)");
        assertThat(screen.text()).contains("╭Files").doesNotContain("start /ticket respond");
    }

    @Test
    void shouldKeepTheRespondDialogUpOnAnyOtherKeyUntilEscapeClosesTheReviewWithTheTicketSelected() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writingThatWrites(writtenView(noPendingReviewView())), hosting);
        shell.press(key('S'));
        shell.press(ctrl('s'));
        shell.press(key('y'));

        shell.press(key('j'));
        shell.press(key('q'));
        assertThat(Screen.of(shell).text()).contains("start /ticket respond PROJ-1?", "╭Files");
        assertThat(shell.quitRequested()).isFalse();
        shell.press(namedKey(KeyCode.ESCAPE));

        assertThat(Screen.of(shell).text()).contains("╭Board").doesNotContain("start /ticket respond", "╭Files");
        assertThat(Screen.of(shell).reversedText()).anySatisfy(row -> assertThat(row).contains("PROJ-1"));
        verifyNoInteractions(hosting);
    }

    @Test
    void shouldKeepTheLastLineTypedAndTheCaretOnScreenWhenTheSummaryOutgrowsTheMainView() {
        Shell shell = shellSubmitting(inReviewSlugTicketView(), mock(PendingReviewCommand.class), mock(HostedSessionCommand.class));
        shell.press(key('S'));
        Screen.of(shell);
        typed(shell, "\n".repeat(30) + "the last line");

        Screen screen = Screen.of(shell);

        assertThat(screen.text()).contains("│ the last line");
        assertThat(screen.cursor()).hasValueSatisfying(at -> assertThat(at.y()).isLessThan(screen.lines().size()));
    }

    @Test
    void shouldSubmitTheSummaryAsItWasWhenYWasPressedThoughItIsTypedAgainBeforeTheSubmitRuns() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        HoldableTasksFixture background = new HoldableTasksFixture();
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class), background);
        shell.press(key('S'));
        typed(shell, "!");
        shell.press(ctrl('s'));
        background.startHolding();
        shell.press(key('y'));
        shell.press(key('S'));
        typed(shell, "?");
        shell.press(ctrl('s'));
        shell.press(key('n'));

        background.releaseInOrder();

        verify(writing).submit(new PendingSubmitPayload(
                project().root(), pullRequestNumber(), new Summary("Looks close. Two nits.\nThe second is about naming.!")));
    }
}
