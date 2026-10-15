package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewIn;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.linkedPastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.startingTicketHostedSessionView;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.strongLink;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

@ExtendWith(MockitoExtension.class)
class ShellTicketStartTest {

    @Mock
    private HostedSessionCommand hosting;

    @Test
    void shouldEndATicketsSectionWithANewRowNamingTheVerbToStart() {
        Shell shell = shellOnTicket(ticketViewIn(Status.TODO), hosting);

        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText()).contains("n new: /ticket start");
    }

    @Test
    void shouldNameThePreselectedVerbOnTheNewRow() {
        Shell shell = shellOnTicket(ticketViewIn(Status.IN_REVIEW), hosting);
        shell.press(alt('j'));
        shell.mergedPullRequestsChanged(
                dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsViewFixture.mergedPullRequestsViewOn(
                        "PROJ-1"));

        assertThat(Screen.of(shell).boardPanelText()).contains("n new: /ticket done");
    }

    @Test
    void shouldOfferNoNewRowOnADoneTicket() {
        Shell shell = shellOnTicket(doneTicketView(), hosting);

        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText()).contains("no session").doesNotContain("n new");
    }

    @Test
    void shouldOpenThePickerWithEnterOnTheNewRow() {
        Shell shell = shellOnTicket(ticketViewIn(Status.TODO), hosting);
        shell.press(enter());

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("╭ PROJ-1 · todo").contains("│ › start");
    }

    @Test
    void shouldListAStartingSessionUnderItsTicketFromItsFirstFrame() {
        Shell shell = shellOnTicket(
                ticketViewIn(Status.TODO),
                sessionsViewWithTabs(startingTicketHostedSessionView()),
                hosting,
                blankScreens());
        shell.press(alt('j'));

        assertThat(Screen.of(shell).boardPanelText()).containsSubsequence("PROJ-1", "starting", "n new: /ticket start");
    }

    @Test
    void shouldFocusTheTabOfAStartingSessionWithEnter() {
        Shell shell = shellOnTicket(
                ticketViewIn(Status.TODO),
                sessionsViewWithTabs(startingTicketHostedSessionView()),
                hosting,
                blankScreens());
        shell.press(enter());
        shell.press(key('k'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldResumeATicketsPastSessionThroughTheTicketsWorktree() {
        when(hosting.resumeInTicketWorktree(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellOnTicket(
                ticketViewIn(Status.TODO),
                sessionsViewOf(List.of(), List.of(linkedPastSessionView(1, strongLink("PROJ-1", "start")))),
                hosting,
                blankScreens());
        shell.press(alt('j'));

        shell.press(enter());

        ArgumentCaptor<ResumeTicketHostedPayload> payload = ArgumentCaptor.forClass(ResumeTicketHostedPayload.class);
        verify(hosting).resumeInTicketWorktree(payload.capture());
        assertThat(payload.getValue().branch()).isEqualTo(ticketBranch());
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
    }

    @Test
    void shouldStartTheVerbOffTheRenderThread() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = new Shell(
                dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf(ticketViewIn(Status.TODO)),
                dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView(),
                emptySessionsView(),
                dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsViewFixture.checkingPullRequestsView(),
                hosting,
                blankScreens(),
                mock(dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery.class),
                queued::add);
        shell.press(key('j'));
        shell.press(key('j'));
        shell.press(key('n'));

        shell.press(enter());

        verify(hosting, never()).startTicket(any());
        assertThat(queued).as("git adds the worktree there, and the frame must not wait for it").hasSize(1);
    }

    @Test
    void shouldListNewInTheKeyBarOnATicketAndNotOnAHeader() {
        Shell shell = shellOnTicket(ticketViewIn(Status.TODO), hosting);
        assertThat(Screen.of(shell).lines().getLast()).contains("n new", "N new");

        shell.press(key('k'));

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("n new").contains("N new");
    }

    @Test
    void shouldListThePickersKeysInTheKeyBarWhileItIsOpen() {
        Shell shell = shellOnTicket(ticketViewIn(Status.TODO), hosting);
        shell.press(key('n'));

        assertThat(Screen.of(shell).lines().getLast()).contains("j/k move", "enter start", "esc cancel");
    }
}
