package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.projectRoot;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.secondTitle;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.oneTabInDockWithReviews;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.readingBoard;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellRefreshing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Modifiers;

class ShellRefreshTest {

    private static final int WIDE = 120;
    private static final int HEIGHT = 24;

    private BoardQuery boards;
    private DocsQuery docs;
    private ReviewQuery reviews;
    private SessionQuery screens;

    @BeforeEach
    void createQueries() {
        boards = mock(BoardQuery.class);
        docs = mock(DocsQuery.class);
        reviews = mock(ReviewQuery.class);
        screens = blankScreens();
    }

    @Test
    void shouldReadTheBoardAgainOnR() {
        when(boards.boardReadAgainFor(readingBoard()))
                .thenReturn(Optional.of(boardViewOf(ticketViewBuilder().title(secondTitle()).build())));
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());

        shell.press(key('R'));

        assertThat(Screen.of(shell).boardPanelText()).contains("#1 Plan the next").doesNotContain("#1 A board");
    }

    @Test
    void shouldReadTheDocsTreeAgainOnR() {
        when(docs.docsFor(new DocsPayload(projectRoot().value()))).thenReturn(docsView());
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());
        shell.press(alt('j'));
        shell.press(alt('j'));

        shell.press(key('R'));

        assertThat(Screen.of(shell).boardPanelText()).contains("context-map.md");
    }

    @Test
    void shouldFetchThePullRequestsAgainOnR() {
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());

        shell.press(key('R'));

        verify(reviews).refresh();
    }

    @Test
    void shouldReadTheSessionsAndTranscriptsAgainOnR() {
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView(), secondTicketView());

        shell.press(key('R'));

        verify(screens)
                .refresh(new SessionPayload(
                        projectRoot().value(), List.of(new LinkedTicketId("1"), new LinkedTicketId("2"))));
    }

    @Test
    void shouldReadNothingOnTheRenderThreadOnR() {
        List<Runnable> queued = new ArrayList<>();
        Shell shell = shellRefreshing(boards, docs, reviews, screens, queued::add, boardViewOf(ticketView()));

        shell.press(key('R'));

        verify(boards, never()).boardReadAgainFor(any());
        verify(docs, never()).docsFor(any());
        verify(screens, never()).refresh(any());
        verify(reviews, never()).refresh();
        assertThat(queued).as("git, gh and the filesystem are asked for on a thread of their own").hasSize(1);
    }

    @Test
    void shouldAnswerRInTheKeyBar() {
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());

        assertThat(Screen.of(shell.render(), WIDE, HEIGHT).lines().getLast()).contains("q quit", "R refresh");
    }

    @Test
    void shouldNotFlashOrSayItRefreshed() {
        when(boards.boardReadAgainFor(readingBoard())).thenReturn(Optional.of(boardViewOf(ticketView())));
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());
        String before = Screen.of(shell).text();

        shell.press(key('R'));

        assertThat(Screen.of(shell).text()).as("a refresh shows only as the rows that changed").isEqualTo(before);
    }

    @Test
    void shouldLeaveRToTheVerbPickerWhileItIsOpen() {
        Shell shell = shellRefreshing(boards, docs, reviews, screens, ticketView());
        shell.press(key('j'));
        shell.press(key('n'));

        shell.press(key('R'));

        verifyNoInteractions(reviews);
    }

    @Test
    void shouldSendRToTheSessionAndRefreshNothingWhileATabHasFocus() {
        HostedSessionCommand hosting = mock(HostedSessionCommand.class);
        HostedEmulatorCommand emulating = mock(HostedEmulatorCommand.class);
        Shell shell = oneTabInDockWithReviews(hosting, emulating, screens, reviews);

        shell.press(key('R'));

        verify(emulating).send(new HostedInputPayload(hostedSessionIdOf(1), typed('R', Modifiers.none())));
        verifyNoInteractions(reviews);
        verify(screens, never()).refresh(any());
    }
}
