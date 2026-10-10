package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryViewFixture.entryView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.bareTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.bodylessTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.datedTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.doneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longDoneTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longSecondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.secondTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.thematicBreakTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.untitledTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.wrappingTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellFocusedOnMainViewOf;
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

        assertThat(Screen.of(shell.render(), 80, 30).boardPanelText())
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
                .contains("PROJ-1 · in-review · board · feature", "A board from the backlog");
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
                .contains("PROJ-1 · draft")
                .doesNotContain("draft ·");
    }

    @Test
    void shouldShowEveryOtherFrontmatterKeyInFileOrderAlignedUnderTheFacts() {
        Shell shell = pressed(shellOn(datedTicketView()), key('j'), 1);

        assertThat(Screen.of(shell).mainViewRows())
                .startsWith("PROJ-1 · draft · board · feature")
                .containsSubsequence("created     2026-10-10", "blocked_by  [PROJ-8, PROJ-9]");
    }

    @Test
    void shouldShowAKeyWrittenWithNothingAfterItBesideItsName() {
        TicketView emptyValue = ticketViewBuilder().frontmatter(List.of(entryView("parent", ""))).build();

        assertThat(Screen.of(pressed(shellOn(emptyValue), key('j'), 1)).mainViewRows()).contains("parent");
    }

    @Test
    void shouldDrawARuleBetweenTheKeysAndTheBody() {
        Shell shell = pressed(shellOn(datedTicketView()), key('j'), 1);

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence("blocked_by  [PROJ-8, PROJ-9]", "─".repeat(50), "A board from the backlog");
    }

    @Test
    void shouldRenderTheBodyAsMarkdownWithoutTheYamlBlock() {
        Shell shell = pressed(shellOn(ticketView()), key('j'), 1);

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("A board from the backlog").doesNotContain("#"))
                .noneSatisfy(row -> assertThat(row).containsAnyOf("---", "id: ", "status: "));
    }

    @Test
    void shouldRenderADelimiterInTheBodyAsAThematicBreak() {
        Shell shell = pressed(shellOn(thematicBreakTicketView()), key('j'), 1);

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence("above", "─".repeat(50), "below")
                .noneSatisfy(row -> assertThat(row).contains("---"));
    }

    @Test
    void shouldSayNoBodyInDimTextUnderTheRuleWhenThereIsNone() {
        Shell shell = pressed(shellOn(bodylessTicketView()), key('j'), 1);

        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).containsSubsequence("─".repeat(50), "no body");
        assertThat(screen.dimText()).contains("no body");
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
        Shell shell = pressed(pressed(shellOn(), alt('l'), 1), alt('j'), 1);

        assertThat(Screen.of(shell).lines().getLast())
                .contains("h/l panel", "alt+h/j/k/l focus", "q quit")
                .doesNotContain("j/k move")
                .doesNotContain("enter fold");
    }

    @Test
    void shouldScrollTheMainViewOneRowWithJ() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());

        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("──────");
    }

    @Test
    void shouldScrollTheMainViewBackUpOneRowWithK() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        pressed(shell, key('j'), 2);

        shell.press(key('k'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("──────");
    }

    @Test
    void shouldStayAtTheTopWhenKIsPressedThere() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());

        shell.press(key('k'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldStayAtTheBottomWhenJIsPressedThere() {
        Shell shell = pressed(shellFocusedOnMainViewOf(longTicketView()), key('G'), 1);
        List<String> atTheBottom = Screen.of(shell).mainViewRows();

        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows()).isEqualTo(atTheBottom);
        assertThat(atTheBottom.getLast()).contains("Row 60");
    }

    @Test
    void shouldComeBackUpInOneKeyAfterPressingJPastTheBottom() {
        Shell shell = pressed(shellFocusedOnMainViewOf(longTicketView()), key('j'), 200);
        Screen.of(shell);

        shell.press(key('k'));

        assertThat(Screen.of(shell).mainViewRows().getLast())
                .as("the offset never rested beyond the last row, so one k moves the view")
                .contains("Row 59");
    }

    @Test
    void shouldNotScrollATicketShorterThanTheView() {
        Shell shell = shellFocusedOnMainViewOf(ticketView());
        List<String> before = Screen.of(shell).mainViewRows();

        pressed(shell, key('j'), 5);
        shell.press(ctrl('d'));
        shell.press(key('G'));

        assertThat(Screen.of(shell).mainViewRows()).isEqualTo(before);
    }

    @Test
    void shouldScrollHalfTheViewWithCtrlD() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        int viewHeight = Screen.of(shell).mainViewRows().size();

        shell.press(ctrl('d'));

        assertThat(Screen.of(shell).mainViewRows().getFirst())
                .as("two head rows precede the body, so row %d is item %d", viewHeight / 2, viewHeight / 2 - 1)
                .contains("Row %02d".formatted(viewHeight / 2 - 1));
    }

    @Test
    void shouldScrollHalfTheViewBackWithCtrlU() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        pressed(shell, ctrl('d'), 3);

        pressed(shell, ctrl('u'), 3);

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldClampCtrlDNearTheBottom() {
        Shell clamped = pressed(shellFocusedOnMainViewOf(longTicketView()), key('G'), 1);
        pressed(clamped, ctrl('d'), 1);
        Shell reference = pressed(shellFocusedOnMainViewOf(longTicketView()), key('G'), 1);

        pressed(clamped, ctrl('u'), 1);
        pressed(reference, ctrl('u'), 1);

        assertThat(Screen.of(clamped).mainViewRows())
                .as("ctrl+d past the bottom left the offset at the maximum, so ctrl+u lands where it does from there")
                .isEqualTo(Screen.of(reference).mainViewRows());
    }

    @Test
    void shouldClampCtrlUNearTheTop() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        shell.press(key('j'));

        shell.press(ctrl('u'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldGoToTheBottomWithBigGAndBackToTheTopWithG() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());

        shell.press(key('G'));
        assertThat(Screen.of(shell).mainViewRows().getLast()).contains("Row 60");
        shell.press(key('g'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldReachTheLastWrappedLineOfALongParagraph() {
        Shell shell = pressed(shellFocusedOnMainViewOf(wrappingTicketView()), key('G'), 1);

        assertThat(Screen.of(shell).mainViewRows().getLast())
                .as("the scroll limit agrees with how many rows the wrapped body renders")
                .contains("THE-END");
    }

    @Test
    void shouldNotMoveTheBoardSelectionWithTheMainViewScrollKeys() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        List<String> before = Screen.of(shell).boardPanelLines();

        pressed(shell, key('j'), 3);
        shell.press(ctrl('d'));
        shell.press(key('G'));
        shell.press(key('k'));
        shell.press(key('h'));

        assertThat(Screen.of(shell).boardPanelLines()).isEqualTo(before);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("PROJ-1");
    }

    @Test
    void shouldResetTheMainViewToTheTopWhenAnotherTicketIsSelected() {
        Shell shell = shellOn(longTicketView(), longSecondTicketView());
        shell.press(key('j'));
        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('G'));
        Screen.of(shell);
        shell.press(key('h'));

        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-2 · draft");
    }

    @Test
    void shouldKeepTheScrollWhenTheSelectionDoesNotMove() {
        Shell shell = pressed(shellOn(longDoneTicketView()), key('j'), 4);
        shell.press(enter());
        shell.press(key('j'));
        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('G'));
        Screen.of(shell);
        shell.press(key('h'));

        shell.press(key('j'));
        shell.press(key('l'));

        assertThat(Screen.of(shell).mainViewRows().getFirst())
                .as("j on the last board row moves nothing, so the main view stays where it was scrolled")
                .doesNotStartWith("PROJ-1 · done");
        assertThat(Screen.of(shell).mainViewRows().getLast()).contains("Row 60");
    }

    @Test
    void shouldResetTheMainViewWhenTheSelectionLeavesAndComesBack() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        shell.press(key('G'));
        Screen.of(shell);
        shell.press(key('h'));

        shell.press(key('k'));
        shell.press(key('j'));
        shell.press(key('l'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldLeaveTheMainViewScrollKeysAloneWhileTheBoardHasFocus() {
        Shell shell = pressed(shellOn(longTicketView()), key('j'), 1);
        Screen.of(shell);

        assertThat(shell.press(ctrl('d'))).isFalse();
        assertThat(shell.press(key('G'))).isFalse();
        assertThat(Screen.of(shell).mainViewRows().getFirst()).startsWith("PROJ-1 · draft");
    }

    @Test
    void shouldListTheScrollKeysBeforeThePanelKeysWhileTheMainViewHasFocus() {
        Shell shell = pressed(shellOn(), key('l'), 1);

        assertThat(Screen.of(shell.render(), 120, 24).lines().getLast())
                .contains("j/k scroll  ^d/^u half page  g/G top/bottom  h/l panel  alt+h/j/k/l focus  q quit");
    }

    @Test
    void shouldNotListTheScrollKeysWhileTheBoardHasFocus() {
        assertThat(Screen.of(shellOn()).lines().getLast()).doesNotContain("scroll").doesNotContain("half page");
    }

    @Test
    void shouldNotListTheScrollKeysWhileTheDockHasFocus() {
        Shell shell = pressed(pressed(shellOn(), alt('l'), 1), alt('j'), 1);

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("scroll");
    }

    @Test
    void shouldStillShowAnUnreadableFilesFileAndReasonInTheMainView() {
        Shell shell = pressed(shellOn(boardViewOf(List.of(), List.of(unreadableTicketView()))), key('j'), 6);

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("PROJ-2.md"));
    }

    @Test
    void shouldShowNothingInTheMainViewOnAGroupHeader() {
        assertThat(Screen.of(shellOn(datedTicketView())).mainViewRows()).allSatisfy(row -> assertThat(row).isBlank());
    }
}
