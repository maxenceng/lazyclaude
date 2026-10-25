package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.longTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewNumbered;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedTab;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewWithTabs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.escape;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.oneTabInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellFocusedOnMainViewOf;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellInDock;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOn;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellZoomedOnColumns;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.tamboui.layout.Position;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

@ExtendWith(MockitoExtension.class)
class ShellZoomTest {

    @Mock
    private HostedSessionCommand hosting;

    private final SessionQuery screens = blankScreens();

    @Test
    void shouldMoveTheCursorDownAndUpWithinAColumnAndTheMainViewFollowsIt() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(key('j'));
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-2 · draft");
    }

    @Test
    void shouldKeepTheRowIndexClampedWhenMovingToTheNextColumn() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        pressed(shell, key('j'), 2);

        shell.press(key('l'));
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-4 · todo");
    }

    @Test
    void shouldMoveBackToThePreviousColumnWithH() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(key('l'));
        shell.press(key('h'));
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-1 · draft");
    }

    @Test
    void shouldSkipTheFoldedDoneColumnWhenMovingRight() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        pressed(shell, key('l'), 4);
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-4 · todo").doesNotContain("PROJ-9");
    }

    @Test
    void shouldLeaveWithTheSelectionWhereTheCursorIsOnZ() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        shell.press(key('l'));

        shell.press(key('z'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("Sessions", "PROJ-4 · todo");
        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-4"));
    }

    @Test
    void shouldLeaveWithTheSelectionWhereTheCursorIsOnEscape() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        shell.press(key('j'));

        shell.press(escape());

        assertThat(Screen.of(shell).text()).contains("Sessions", "PROJ-2 · draft");
    }

    @Test
    void shouldLeaveAndFocusTheTicketsSessionsOnEnter() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        shell.press(key('j'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).text()).contains("╭Sessions", "n new: /ticket refine");
    }

    @Test
    void shouldJustLeaveOnEnterWhenTheBoardHasNoTicket() {
        Shell shell = shellOn();
        shell.press(key('z'));

        shell.press(enter());

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldPutTheCursorOnATicketWhenTheSelectionWasOnAHeader() {
        Shell shell = shellOn(ticketViewNumbered(2, Status.TODO));

        shell.press(key('z'));
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-2 · todo");
    }

    @Test
    void shouldRevealATicketInAFoldedGroupWhenTheCursorMovesOntoIt() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT), ticketViewNumbered(2, Status.TODO));
        pressed(shell, key('j'), 2);
        shell.press(enter());

        shell.press(key('z'));
        shell.press(key('l'));
        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("PROJ-2 · todo");
        assertThat(Screen.of(shell).reversedText()).anySatisfy(text -> assertThat(text).contains("PROJ-2"));
    }

    @Test
    void shouldOpenTheColumnsWithAltZFromTheBoardAndCloseThemWithAltZ() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));

        shell.press(alt('z'));
        assertThat(Screen.of(shell).text()).doesNotContain("Sessions");

        shell.press(alt('z'));
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldNotZoomAPanelThatHasNoZoom() {
        Shell shell = shellOn(ticketViewNumbered(1, Status.DRAFT));
        shell.press(alt('j'));

        assertThat(shell.press(key('z'))).isFalse();
        assertThat(shell.press(alt('z'))).isFalse();

        assertThat(Screen.of(shell).text()).contains("Sessions", "Docs");
    }

    @Test
    void shouldZoomTheMainViewBetweenTheStatusLineAndTheKeyBarWithItsDockAsAStrip() {
        Shell shell = shellFocusedOnMainViewOf(ticketViewNumbered(1, Status.DRAFT));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("main view", "dock: no hosted session").doesNotContain("Sessions", "Docs");
    }

    @Test
    void shouldScrollAZoomedMainViewAndStayZoomed() {
        Shell shell = shellFocusedOnMainViewOf(longTicketView());
        shell.press(key('z'));
        String before = Screen.of(shell).text();

        pressed(shell, key('j'), 3);

        assertThat(Screen.of(shell).text()).isNotEqualTo(before).doesNotContain("Sessions");
    }

    @Test
    void shouldLeaveAZoomedMainViewOnZAndOnEscape() {
        Shell shell = shellFocusedOnMainViewOf(ticketViewNumbered(1, Status.DRAFT));

        shell.press(key('z'));
        shell.press(key('z'));
        assertThat(Screen.of(shell).text()).contains("Sessions");

        shell.press(key('z'));
        shell.press(escape());
        assertThat(Screen.of(shell).text()).contains("Sessions");
        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
    }

    @Test
    void shouldLeaveAZoomedMainViewBeforeHMovesFocusToThePanels() {
        Shell shell = shellFocusedOnMainViewOf(ticketViewNumbered(1, Status.DRAFT));
        shell.press(key('z'));

        shell.press(key('h'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldLeaveTheColumnsBeforeAltJMovesFocusDown() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(alt('j'));

        assertThat(shell.focus()).isEqualTo(Focus.SESSIONS);
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldLeaveTheColumnsBeforeAltLMovesFocusToTheMainView() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(alt('l'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("main view", "Sessions");
    }

    @Test
    void shouldLeaveTheColumnsOnWAndOnAltW() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        shell.press(key('w'));
        assertThat(Screen.of(shell).text()).contains("Sessions");

        shell.press(key('z'));
        shell.press(alt('w'));
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldLeaveTheColumnsBeforeAltMHidesTheMainView() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(alt('m'));

        assertThat(Screen.of(shell).text()).contains("Sessions").doesNotContain("main view");
    }

    @Test
    void shouldLeaveTheColumnsBeforeANewSessionTakesFocusInTheDock() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));

        shell.press(key('N'));
        shell.press(key('z'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldLeaveTheColumnsBeforeNOpensTheVerbPickerOverTheSessions() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.TODO), ticketViewNumbered(9, Status.DONE));

        shell.press(key('n'));

        assertThat(Screen.of(shell).text())
                .as("the verb picker sits over the Sessions panel, so the columns that hid it give way first")
                .contains("Sessions")
                .contains("PROJ-1 · todo")
                .contains("Docs");
    }

    @Test
    void shouldListTheZoomsOwnKeysInTheKeyBar() {
        Shell shell = shellZoomedOnColumns(
                ticketViewNumbered(1, Status.DRAFT),
                ticketViewNumbered(2, Status.DRAFT),
                ticketViewNumbered(3, Status.DRAFT),
                ticketViewNumbered(4, Status.TODO),
                ticketViewNumbered(9, Status.DONE));
        assertThat(Screen.of(shell).text()).contains("h/l column  j/k move  enter pick  z/esc back");

        shell.press(key('z'));
        assertThat(Screen.of(shell).text()).doesNotContain("enter pick");
    }

    @Test
    void shouldListTheMainViewZoomKeysInTheKeyBar() {
        Shell shell = shellFocusedOnMainViewOf(ticketViewNumbered(1, Status.DRAFT));

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("j/k scroll");
        assertThat(Screen.of(shell).lines().getLast()).contains("z/esc back");
    }

    @Test
    void shouldOfferTheZoomInTheKeyBarOfTheBoard() {
        assertThat(Screen.of(shellOn(ticketViewNumbered(1, Status.DRAFT)).render(), 200, 24).lines().getLast()).contains("z zoom");
    }

    @Test
    void shouldZoomTheDockWithAltZFromASessionAndResizeItsPtyToTheSpace() {
        Shell shell = oneTabInDock(hosting, screens);

        shell.press(alt('z'));
        String zoomed = Screen.of(shell).text();

        assertThat(zoomed).contains("╭dock").doesNotContain("╭Sessions", "╭Board", "╭main view");
        ArgumentCaptor<ResizePayload> resized = ArgumentCaptor.forClass(ResizePayload.class);
        verify(hosting, times(2)).resize(resized.capture());
        assertThat(resized.getAllValues().getLast().size().height().cells())
                .isGreaterThan(resized.getAllValues().getFirst().size().height().cells());
        assertThat(resized.getAllValues().getLast().size().width().cells())
                .isGreaterThan(resized.getAllValues().getFirst().size().width().cells());
    }

    @Test
    void shouldResizeTheSessionBackWhenTheZoomEnds() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));
        Screen.of(shell);

        shell.press(alt('z'));
        Screen.of(shell);

        ArgumentCaptor<ResizePayload> resized = ArgumentCaptor.forClass(ResizePayload.class);
        verify(hosting, times(3)).resize(resized.capture());
        assertThat(resized.getAllValues().getLast().size()).isEqualTo(resized.getAllValues().getFirst().size());
    }

    @Test
    void shouldHandAPlainZToTheSessionAndNeverZoomOnIt() {
        Shell shell = oneTabInDock(hosting, screens);

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("Board");
    }

    @Test
    void shouldKeepTheDockZoomedWhileAltHAndAltLStepThroughTabs() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy()), hostedTab(2, "two", new Busy())), hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('l'));
        Screen afterNext = Screen.of(shell);
        shell.press(alt('h'));
        Screen afterPrevious = Screen.of(shell);

        assertThat(afterNext.text()).doesNotContain("Sessions");
        assertThat(afterNext.reversedTextOnLine(afterNext.lineOf("╭dock"))).isEqualTo("two");
        assertThat(afterPrevious.text()).doesNotContain("Sessions");
        assertThat(afterPrevious.reversedTextOnLine(afterPrevious.lineOf("╭dock"))).isEqualTo("one");
    }

    @Test
    void shouldLeaveTheDockZoomBeforeAltHLeavesTheFirstTab() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('h'));

        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
        assertThat(Screen.of(shell).text()).contains("Sessions");
    }

    @Test
    void shouldLeaveTheDockZoomBeforeAltKGoesUpToTheMainView() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('k'));

        assertThat(shell.focus()).isEqualTo(Focus.MAIN);
        assertThat(Screen.of(shell).text()).contains("Sessions", "main view");
    }

    @Test
    void shouldLeaveTheDockZoomBeforeAltMShowsOrHidesTheMainView() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('m'));

        assertThat(Screen.of(shell).text()).contains("╭Sessions").doesNotContain("╭main view");
    }

    @Test
    void shouldLeaveTheDockZoomOnAltWAndKeepTheKeyFromClaude() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('w'));

        assertThat(Screen.of(shell).text()).contains("Sessions");
        verify(hosting, never()).send(any());
    }

    @Test
    void shouldKeepTheDockZoomedOnAltJBecauseNothingIsBelow() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.press(alt('j'));

        assertThat(Screen.of(shell).text()).doesNotContain("Sessions");
    }

    @Test
    void shouldEndTheDockZoomWhenTheLastHostedSessionEnds() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        shell.sessionsChanged(sessionsViewWithTabs());

        assertThat(Screen.of(shell).text()).contains("Sessions", "Board");
        assertThat(shell.focus()).isEqualTo(Focus.BOARD);
    }

    @Test
    void shouldKeepTheDockZoomedWhenAnotherSessionEndsAndATabRemains() {
        Shell shell = shellInDock(
                sessionsViewWithTabs(hostedTab(1, "one", new Busy()), hostedTab(2, "two", new Busy())), hosting, screens);
        shell.press(alt('z'));

        shell.sessionsChanged(sessionsViewWithTabs(hostedTab(2, "two", new Busy())));

        assertThat(Screen.of(shell).text()).doesNotContain("Sessions");
    }

    @Test
    void shouldListTheDockZoomKeysInTheKeyBar() {
        Shell shell = oneTabInDock(hosting, screens);

        shell.press(alt('z'));

        assertThat(Screen.of(shell).lines().getLast()).contains("alt+z back");
    }

    @Test
    void shouldSwallowCtrlZWhileTheDockIsZoomed() {
        Shell shell = oneTabInDock(hosting, screens);
        shell.press(alt('z'));

        assertThat(shell.press(ctrl('z'))).isTrue();

        assertThat(Screen.of(shell).text()).doesNotContain("Sessions");
        verify(hosting, never()).send(any());
    }

    @Test
    void shouldOpenTheColumnsOnABoardRebuiltFromNothing() {
        Shell shell = shellOn(boardViewOf());

        shell.press(key('z'));

        assertThat(Screen.of(shell).text()).contains("draft (0)");
    }

    @Test
    void shouldHandNoMouseEventToTheSessionWhileItsPaneIsHiddenByTheColumns() {
        Shell shell = shellHosting(sessionsViewWithTabs(hostedTab(1, "one", new Busy())), hosting, screens);
        Position origin = Screen.of(shell).dockContentOrigin();
        shell.press(key('z'));
        Screen.of(shell);

        assertThat(shell.mouse(MouseEvent.press(MouseButton.LEFT, origin.x() + 4, origin.y() + 1)))
                .as("the columns cover where the pane was drawn, so a click there is not the session's")
                .isFalse();
        verify(hosting, never()).send(any());
    }
}
