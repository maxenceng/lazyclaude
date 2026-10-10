package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.bareTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.untitledTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOn;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Title;

class ShellTest {

    @Test
    void shouldGroupTicketsByStatusInLifecycleOrderWithCounts() {
        Shell shell = shellOn(
                ticketViewBuilder().id(new TicketId("A-1")).status(Status.DONE).build(),
                ticketViewBuilder().id(new TicketId("A-2")).status(Status.IN_REVIEW).build(),
                ticketViewBuilder().id(new TicketId("A-3")).status(Status.TODO).build(),
                ticketViewBuilder().id(new TicketId("A-4")).status(Status.DRAFT).build(),
                ticketViewBuilder().id(new TicketId("A-5")).status(Status.DRAFT).build(),
                ticketViewBuilder().id(new TicketId("A-6")).status(Status.IN_PROGRESS).build());

        assertThat(Screen.of(shell).boardPanelText())
                .containsSubsequence(
                        "▾ draft (2)", "A-4", "A-5", "▾ todo (1)", "A-3", "▾ in-progress (1)", "A-6", "▾ in-review (1)",
                        "A-2", "▸ done (1)");
    }

    @Test
    void shouldShowAnEmptyGroupAtZero() {
        assertThat(Screen.of(shellOn(ticketView())).text())
                .contains("▾ todo (0)", "▾ in-progress (0)", "▾ in-review (0)", "▸ done (0)");
    }

    @Test
    void shouldStartWithDoneFolded() {
        assertThat(Screen.of(shellOn(doneTicketView())).text())
                .contains("▸ done (1)")
                .doesNotContain(doneTicketView().id().value());
    }

    @Test
    void shouldUnfoldDoneWithEnterOnItsHeader() {
        Shell shell = pressed(shellOn(doneTicketView()), key('j'), 4);

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("▾ done (1)", doneTicketView().id().value());
    }

    @Test
    void shouldFoldAnOpenGroupWithEnterOnItsHeader() {
        Shell shell = shellOn(ticketView());

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("▸ draft (1)").doesNotContain(ticketView().id().value());
    }

    @Test
    void shouldFoldAGroupThatIsEmpty() {
        Shell shell = pressed(shellOn(), key('j'), 1);

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("▸ todo (0)");
    }

    @Test
    void shouldLeaveATicketRowAloneOnEnter() {
        Shell shell = pressed(shellOn(ticketView()), key('j'), 1);

        shell.press(enter());

        assertThat(Screen.of(shell).text()).contains("▾ draft (1)", ticketView().id().value());
    }

    @Test
    void shouldListUnreadableTicketsLastWithTheirReason() {
        Shell shell = shellOn(boardViewOf(List.of(ticketView()), List.of(unreadableTicketView())));

        assertThat(Screen.of(shell).boardPanelText())
                .containsSubsequence("▸ done (0)", "▾ unreadable (1)", "PROJ-2.md — status is");
    }

    @Test
    void shouldHideTheUnreadableGroupWhenNothingIsUnreadable() {
        assertThat(Screen.of(shellOn()).text()).doesNotContain("unreadable");
    }

    @Test
    void shouldShowATicketWithoutATitleUnderItsId() {
        assertThat(Screen.of(shellOn(untitledTicketView())).boardPanelText()).contains("PROJ-1 PROJ-1");
    }

    @Test
    void shouldMoveTheSelectionDownWithJ() {
        Shell shell = shellOn(ticketView(), secondTicketView());

        pressed(shell, key('j'), 2);

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-2");
    }

    @Test
    void shouldMoveTheSelectionUpWithK() {
        Shell shell = pressed(shellOn(ticketView(), secondTicketView()), key('j'), 2);

        shell.press(key('k'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-1");
    }

    @Test
    void shouldStayOnTheFirstRowWhenKIsPressedThere() {
        Shell shell = shellOn(ticketView());

        shell.press(key('k'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("draft (1)");
    }

    @Test
    void shouldStayOnTheLastRowWhenJIsPressedThere() {
        Shell shell = shellOn(ticketView());

        pressed(shell, key('j'), 20);

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("done (0)");
    }

    @Test
    void shouldKeepTheSelectionVisibleWhenTheBoardIsLongerThanThePanel() {
        List<TicketView> forty = IntStream.rangeClosed(1, 40)
                .mapToObj(number -> ticketViewBuilder()
                        .id(new TicketId("A-%d".formatted(number)))
                        .title(new Title("Ticket %d".formatted(number)))
                        .build())
                .toList();
        Shell shell = shellOn(forty.toArray(TicketView[]::new));

        pressed(shell, key('j'), 30);

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("A-30");
    }

    @Test
    void shouldShowTheSelectedTicketInTheMainView() {
        Shell shell = pressed(shellOn(ticketViewBuilder().status(Status.IN_REVIEW).build()), key('j'), 4);

        assertThat(Screen.of(shell).text())
                .contains("PROJ-1", "A board from the backlog", "status in-review · board · feature");
    }

    @Test
    void shouldShowTheSelectedTicketsDetailRightOfTheBoard() {
        Shell shell = pressed(shellOn(ticketView()), key('j'), 1);

        assertThat(Screen.of(shell).rightColumnLines()).anySatisfy(line -> assertThat(line).contains("A board from the backlog"));
    }

    @Test
    void shouldLeaveTheMainViewEmptyOnAGroupHeader() {
        assertThat(Screen.of(shellOn(ticketView())).rightColumnLines())
                .noneSatisfy(line -> assertThat(line).contains("A board from the backlog"));
    }

    @Test
    void shouldShowAnUnreadableFilesReasonInTheMainViewWhenItIsSelected() {
        Shell shell = pressed(shellOn(boardViewOf(List.of(), List.of(unreadableTicketView()))), key('j'), 6);

        assertThat(Screen.of(shell).rightColumnLines()).anySatisfy(line -> assertThat(line).contains("status is missing"));
    }

    @Test
    void shouldLeaveAMissingContextAndTypeOutOfTheMainView() {
        Shell shell = pressed(shellOn(bareTicketView()), key('j'), 1);

        assertThat(Screen.of(shell).text())
                .as("an absent field is left out, never shown as a blank between separators")
                .contains("status draft")
                .doesNotContain("·");
    }

    @Test
    void shouldTitleTheOtherPanelsAndLeaveThemEmpty() {
        assertThat(Screen.of(shellOn()).text()).contains("Board", "Sessions", "Docs", "Elsewhere", "dock", "main view");
    }

    @Test
    void shouldSayThereIsNoHostedSession() {
        assertThat(Screen.of(shellOn()).text()).contains("no hosted session");
    }

    @Test
    void shouldLeaveTheStatusLineEmpty() {
        assertThat(Screen.of(shellOn()).lines().getFirst()).isBlank();
    }

    @Test
    void shouldStartWithFocusOnTheBoard() {
        assertThat(Screen.of(shellOn()).cyanText()).anySatisfy(text -> assertThat(text).contains("Board"));
    }

    @Test
    void shouldMoveFocusToTheMainViewWithL() {
        Shell shell = shellOn();

        shell.press(key('l'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).cyanText()).anySatisfy(text -> assertThat(text).contains("main view"));
    }

    @Test
    void shouldMoveFocusBackToTheBoardWithH() {
        Shell shell = pressed(shellOn(), key('l'), 1);

        shell.press(key('h'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldReturnToTheLeftPanelFocusWasLastOn() {
        Shell shell = pressed(pressed(shellOn(), alt('j'), 1), key('l'), 1);

        shell.press(key('h'));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
    }

    @Test
    void shouldMoveFocusDownTheLeftPanelsWithAltJ() {
        Shell shell = shellOn();

        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.DOCS);
        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.ELSEWHERE);
        shell.press(alt('j'));
        assertThat(shell.focus()).as("focus does not wrap").isEqualTo(Focus.ELSEWHERE);
    }

    @Test
    void shouldMoveFocusUpTheLeftPanelsWithAltK() {
        Shell shell = pressed(shellOn(), alt('j'), 1);

        pressed(shell, alt('k'), 2);

        assertThat(shell.focus()).as("focus does not wrap").isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldMoveFocusBetweenMainViewAndDockWithAltJAndAltK() {
        Shell shell = pressed(shellOn(), alt('l'), 1);

        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        shell.press(alt('j'));
        assertThat(shell.focus()).isEqualTo(Focus.DOCK);
        shell.press(alt('k'));
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldMoveFocusAcrossColumnsWithAltLAndAltH() {
        Shell shell = shellOn();

        shell.press(alt('l'));
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        shell.press(alt('h'));
        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldNotMoveTheBoardSelectionWhileAnotherPanelHasFocus() {
        Shell shell = pressed(shellOn(ticketView()), key('l'), 1);

        shell.press(key('j'));
        shell.press(key('h'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("draft (1)");
    }

    @Test
    void shouldReserveJAndKForTheBoardOnlyWhenItHasFocus() {
        Shell shell = pressed(shellOn(), key('l'), 1);

        shell.press(key('j'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldQuitOnQWithoutConfirmation() {
        Shell shell = shellOn();

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldQuitOnQFromAnyPanel() {
        Shell shell = pressed(pressed(shellOn(), alt('l'), 1), alt('j'), 1);

        shell.press(key('q'));

        assertThat(shell.quitRequested()).isTrue();
    }

    @Test
    void shouldNotQuitOnAltQ() {
        Shell shell = shellOn();

        shell.press(alt('q'));

        assertThat(shell.quitRequested()).isFalse();
    }

    @Test
    void shouldIgnoreKeysItHasNoMeaningFor() {
        assertThat(shellOn().press(key('x'))).as("an unhandled key falls through to the runner").isFalse();
    }

    @Test
    void shouldListTheBoardKeysOnAGroupHeader() {
        assertThat(Screen.of(shellOn()).lines().getLast())
                .contains("j/k move", "enter fold", "h/l panel", "alt+h/j/k/l focus", "q quit");
    }

    @Test
    void shouldNotOfferFoldOnATicketRow() {
        Shell shell = pressed(shellOn(ticketView()), key('j'), 1);

        assertThat(Screen.of(shell).lines().getLast()).contains("j/k move", "q quit").doesNotContain("enter fold");
    }

    @Test
    void shouldListNoBoardKeysWhenAnotherPanelHasFocus() {
        Shell shell = pressed(shellOn(), key('l'), 1);

        assertThat(Screen.of(shell).lines().getLast())
                .contains("h/l panel", "alt+h/j/k/l focus", "q quit")
                .doesNotContain("j/k move")
                .doesNotContain("enter fold");
    }
}
