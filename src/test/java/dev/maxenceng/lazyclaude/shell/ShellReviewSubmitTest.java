package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.inReviewSlugTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.slugTicketView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.noPendingReviewView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.refusedWriteView;
import static dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewViewFixture.writtenView;
import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.namedKey;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.shellSubmitting;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.typed;
import static dev.maxenceng.lazyclaude.shell.ShellReviewFixture.writingThatWrites;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingSummaryPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestPayload;
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
    void shouldSaveTheSummaryThenAskOnTheStatusLineWhetherToSubmitItAsAComment() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        typed(shell, "!");

        shell.press(ctrl('s'));

        verify(writing).summarise(new PendingSummaryPayload(
                project().root(), pullRequestNumber(), new Summary("Looks close. Two nits.\nThe second is about naming.!")));
        assertThat(Screen.of(shell).statusLine()).contains("submit review as Comment? y/n");
        assertThat(Screen.of(shell).text()).doesNotContain("^s save");
    }

    @Test
    void shouldLeaveTheSummarySavedAndTheReviewPendingOnNOrEscapeAtTheConfirm() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('n'));
        assertThat(Screen.of(shell).statusLine()).doesNotContain("submit review");
        shell.press(key('S'));
        shell.press(ctrl('s'));
        shell.press(namedKey(KeyCode.ESCAPE));

        assertThat(Screen.of(shell).statusLine()).doesNotContain("submit review");
        assertThat(Screen.of(shell).text()).contains("╭Files");
        verify(writing, never()).submit(any());
    }

    @Test
    void shouldKeepTheSummaryBoxOpenWithWhyWhenItCannotBeSavedAndAskNothing() {
        PendingReviewCommand writing = writingThatWrites(refusedWriteView());
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));

        shell.press(ctrl('s'));

        assertThat(Screen.of(shell).text()).contains("not saved (gh: not logged in)");
        assertThat(Screen.of(shell).statusLine()).doesNotContain("submit review");
    }

    @Test
    void shouldSubmitOnYAndOfferToStartRespondInANewTabWhenTheTicketsStatusAllowsIt() {
        PendingReviewCommand writing = writingThatWrites(writtenView(noPendingReviewView()));
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('y'));
        Screen screen = Screen.of(shell);

        verify(writing).submit(new PullRequestPayload(project().root(), pullRequestNumber()));
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
        when(writing.summarise(any())).thenReturn(writtenView(noPendingReviewView()));
        when(writing.submit(any())).thenReturn(refusedWriteView());
        Shell shell = shellSubmitting(inReviewSlugTicketView(), writing, mock(HostedSessionCommand.class));
        shell.press(key('S'));
        shell.press(ctrl('s'));

        shell.press(key('y'));
        Screen screen = Screen.of(shell);

        assertThat(screen.lines().getLast()).contains("not submitted (gh: not logged in)");
        assertThat(screen.text()).contains("╭Files").doesNotContain("start /ticket respond");
    }
}
