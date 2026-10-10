package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.BoardView;
import dev.maxenceng.lazyclaude.board.published.StatusGroupView;
import dev.maxenceng.lazyclaude.board.published.TicketView;
import dev.maxenceng.lazyclaude.board.published.UnreadableTicketView;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

class ShellTest {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;

    private static final List<String> LIFECYCLE = List.of("draft", "todo", "in-progress", "in-review", "done");
    private static final String FINISHED = "done";

    private static TicketView ticket(String id, String status, String title) {
        return new TicketView(id, title == null ? id : title, status, "board", "feature");
    }

    private static BoardView board(List<TicketView> tickets, List<UnreadableTicketView> unreadable) {
        List<StatusGroupView> groups = LIFECYCLE.stream()
                .map(status -> new StatusGroupView(
                        status,
                        status.equals(FINISHED),
                        tickets.stream().filter(ticket -> ticket.status().equals(status)).toList()))
                .toList();
        return new BoardView(groups, unreadable);
    }

    private static Shell shellOn(BoardView board) {
        return new Shell(board);
    }

    private static Shell shellOn(TicketView... tickets) {
        return shellOn(board(List.of(tickets), List.of()));
    }

    private static KeyEvent key(char c) {
        return KeyEvent.ofChar(c);
    }

    private static KeyEvent alt(char c) {
        return KeyEvent.ofChar(c, KeyModifiers.ALT);
    }

    private static Screen screen(Shell shell) {
        return Screen.of(shell.render(), WIDTH, HEIGHT);
    }

    private static List<String> boardPanelLines(Screen screen) {
        List<String> found = new ArrayList<>();
        for (String line : screen.lines()) {
            int bar = line.indexOf('│');
            if (bar >= 0 && line.indexOf('│', bar + 1) >= 0) {
                found.add(line.substring(bar + 1, line.indexOf('│', bar + 1)).stripTrailing());
            }
        }
        return found;
    }

    @Test
    void shouldGroupTicketsByStatusInLifecycleOrderWithCounts() {
        Shell shell = shellOn(
                ticket("A-1", "done", "Old"),
                ticket("A-2", "in-review", "Review"),
                ticket("A-3", "todo", "Todo"),
                ticket("A-4", "draft", "Draft one"),
                ticket("A-5", "draft", "Draft two"),
                ticket("A-6", "in-progress", "Working"));

        String board = String.join("\n", boardPanelLines(screen(shell)));

        assertThat(board)
                .containsSubsequence(
                        "▾ draft (2)", "A-4", "A-5", "▾ todo (1)", "A-3", "▾ in-progress (1)", "A-6", "▾ in-review (1)",
                        "A-2", "▸ done (1)");
    }

    @Test
    void shouldShowAnEmptyGroupAtZero() {
        Shell shell = shellOn(ticket("A-1", "draft", "Only"));

        assertThat(screen(shell).text()).contains("▾ todo (0)", "▾ in-progress (0)", "▾ in-review (0)", "▸ done (0)");
    }

    @Test
    void shouldStartWithDoneFolded() {
        Shell shell = shellOn(ticket("A-1", "done", "Shipped"));

        assertThat(screen(shell).text()).contains("▸ done (1)").doesNotContain("A-1");
    }

    @Test
    void shouldUnfoldDoneWithEnterOnItsHeader() {
        Shell shell = shellOn(ticket("A-1", "done", "Shipped"));
        for (int i = 0; i < 4; i++) {
            shell.press(key('j'));
        }

        shell.press(KeyEvent.ofKey(KeyCode.ENTER));

        assertThat(screen(shell).text()).contains("▾ done (1)", "A-1");
    }

    @Test
    void shouldFoldAnOpenGroupWithEnterOnItsHeader() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));

        shell.press(KeyEvent.ofKey(KeyCode.ENTER));

        assertThat(screen(shell).text()).contains("▸ draft (1)").doesNotContain("A-1");
    }

    @Test
    void shouldFoldAGroupThatIsEmpty() {
        Shell shell = shellOn();
        shell.press(key('j'));

        shell.press(KeyEvent.ofKey(KeyCode.ENTER));

        assertThat(screen(shell).text()).contains("▸ todo (0)");
    }

    @Test
    void shouldLeaveATicketRowAloneOnEnter() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));
        shell.press(key('j'));

        shell.press(KeyEvent.ofKey(KeyCode.ENTER));

        assertThat(screen(shell).text()).contains("▾ draft (1)", "A-1");
    }

    @Test
    void shouldListUnreadableTicketsLastWithTheirReason() {
        BoardView board = board(
                List.of(ticket("A-1", "draft", "Fine")),
                List.of(new UnreadableTicketView("A-2.md", "bad")));

        String boardText = String.join("\n", boardPanelLines(screen(shellOn(board))));

        assertThat(boardText).containsSubsequence("▸ done (0)", "▾ unreadable (1)", "A-2.md — bad");
    }

    @Test
    void shouldHideTheUnreadableGroupWhenNothingIsUnreadable() {
        assertThat(screen(shellOn()).text()).doesNotContain("unreadable");
    }

    @Test
    void shouldShowATicketWithoutATitleUnderItsId() {
        Shell shell = shellOn(ticket("A-1", "draft", null));

        assertThat(String.join("\n", boardPanelLines(screen(shell)))).contains("A-1 A-1");
    }

    @Test
    void shouldMoveTheSelectionDownWithJ() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"), ticket("A-2", "draft", "Plan"));

        shell.press(key('j'));
        shell.press(key('j'));

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("A-2");
    }

    @Test
    void shouldMoveTheSelectionUpWithK() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"), ticket("A-2", "draft", "Plan"));
        shell.press(key('j'));
        shell.press(key('j'));

        shell.press(key('k'));

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("A-1");
    }

    @Test
    void shouldStayOnTheFirstRowWhenKIsPressedThere() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));

        shell.press(key('k'));

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("draft (1)");
    }

    @Test
    void shouldStayOnTheLastRowWhenJIsPressedThere() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));
        for (int i = 0; i < 20; i++) {
            shell.press(key('j'));
        }

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("done (0)");
    }

    @Test
    void shouldKeepTheSelectionVisibleWhenTheBoardIsLongerThanThePanel() {
        List<TicketView> tickets = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            tickets.add(ticket("A-" + i, "draft", "Ticket " + i));
        }
        Shell shell = shellOn(board(tickets, List.of()));

        for (int i = 0; i < 30; i++) {
            shell.press(key('j'));
        }

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("A-30");
    }

    @Test
    void shouldShowTheSelectedTicketInTheMainView() {
        Shell shell = shellOn(ticket("A-1", "in-review", "A fine idea"));
        shell.press(key('j'));
        shell.press(key('j'));
        shell.press(key('j'));
        shell.press(key('j'));

        String text = screen(shell).text();

        assertThat(text).contains("A-1", "A fine idea", "status in-review · board · feature");
    }

    @Test
    void shouldShowTheSelectedTicketsDetailRightOfTheBoard() {
        Shell shell = shellOn(ticket("A-1", "draft", "A fine idea"));
        shell.press(key('j'));

        List<String> lines = screen(shell).lines();

        assertThat(lines).anySatisfy(line -> assertThat(line.substring(28)).contains("A fine idea"));
    }

    @Test
    void shouldLeaveTheMainViewEmptyOnAGroupHeader() {
        Shell shell = shellOn(ticket("A-1", "draft", "A fine idea"));

        List<String> lines = screen(shell).lines();

        assertThat(lines).noneSatisfy(line -> assertThat(line.substring(28)).contains("A fine idea"));
    }

    @Test
    void shouldShowAnUnreadableFilesReasonInTheMainViewWhenItIsSelected() {
        BoardView board = board(List.of(), List.of(new UnreadableTicketView("A-2.md", "status is missing")));
        Shell shell = shellOn(board);
        for (int i = 0; i < 6; i++) {
            shell.press(key('j'));
        }

        assertThat(screen(shell).lines()).anySatisfy(line -> assertThat(line.substring(28)).contains("status is missing"));
    }

    @Test
    void shouldTitleTheOtherPanelsAndLeaveThemEmpty() {
        String text = screen(shellOn()).text();

        assertThat(text).contains("Board", "Sessions", "Docs", "Elsewhere", "dock", "main view");
    }

    @Test
    void shouldSayThereIsNoHostedSession() {
        assertThat(screen(shellOn()).text()).contains("no hosted session");
    }

    @Test
    void shouldLeaveTheStatusLineEmpty() {
        assertThat(screen(shellOn()).lines().getFirst()).isBlank();
    }

    @Test
    void shouldStartWithFocusOnTheBoard() {
        assertThat(screen(shellOn()).cyanText()).anySatisfy(text -> assertThat(text).contains("Board"));
    }

    @Test
    void shouldMoveFocusToTheMainViewWithL() {
        Shell shell = shellOn();

        shell.press(key('l'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(screen(shell).cyanText()).anySatisfy(text -> assertThat(text).contains("main view"));
    }

    @Test
    void shouldMoveFocusBackToTheBoardWithH() {
        Shell shell = shellOn();
        shell.press(key('l'));

        shell.press(key('h'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldReturnToTheLeftPanelFocusWasLastOn() {
        Shell shell = shellOn();
        shell.press(alt('j'));
        shell.press(key('l'));

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
        Shell shell = shellOn();
        shell.press(alt('j'));

        shell.press(alt('k'));
        shell.press(alt('k'));

        assertThat(shell.focus()).as("focus does not wrap").isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldMoveFocusBetweenMainViewAndDockWithAltJAndAltK() {
        Shell shell = shellOn();
        shell.press(alt('l'));

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
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));
        shell.press(key('l'));

        shell.press(key('j'));
        shell.press(key('h'));

        assertThat(screen(shell).reversedText()).singleElement().asString().contains("draft (1)");
    }

    @Test
    void shouldReserveJAndKForTheBoardOnlyWhenItHasFocus() {
        Shell shell = shellOn();
        shell.press(key('l'));

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
        Shell shell = shellOn();
        shell.press(alt('l'));
        shell.press(alt('j'));

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
        Shell shell = shellOn();

        boolean handled = shell.press(key('x'));

        assertThat(handled).as("an unhandled key falls through to the runner").isFalse();
    }

    @Test
    void shouldListTheBoardKeysOnAGroupHeader() {
        assertThat(screen(shellOn()).lines().getLast())
                .contains("j/k move", "enter fold", "h/l panel", "alt+h/j/k/l focus", "q quit");
    }

    @Test
    void shouldNotOfferFoldOnATicketRow() {
        Shell shell = shellOn(ticket("A-1", "draft", "Idea"));
        shell.press(key('j'));

        assertThat(screen(shell).lines().getLast()).contains("j/k move", "q quit").doesNotContain("enter fold");
    }

    @Test
    void shouldListNoBoardKeysWhenAnotherPanelHasFocus() {
        Shell shell = shellOn();
        shell.press(key('l'));

        assertThat(screen(shell).lines().getLast())
                .contains("h/l panel", "alt+h/j/k/l focus", "q quit")
                .doesNotContain("j/k move")
                .doesNotContain("enter fold");
    }
}
