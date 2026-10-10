package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * The terminal application itself. It owns the screen and the keyboard, and composes what
 * each context's primary adapter offers; it holds no rules of its own.
 */
public final class Shell extends ToolkitApp {

    private static final int LEFT_COLUMN_PERCENT = 35;
    private static final int SMALL_PANEL_HEIGHT = 3;
    private static final int STATUS_LINE_HEIGHT = 1;
    private static final int KEY_BAR_HEIGHT = 1;

    private static final String BOARD_TITLE = "Board";
    private static final String SESSIONS_TITLE = "Sessions";
    private static final String DOCS_TITLE = "Docs";
    private static final String ELSEWHERE_TITLE = "Elsewhere";
    private static final String MAIN_VIEW_TITLE = "main view";
    private static final String DOCK_TITLE = "dock";
    private static final String NO_HOSTED_SESSION = "no hosted session";
    private static final String EMPTY = "";

    private static final char QUIT = 'q';
    private static final char LEFT = 'h';
    private static final char DOWN = 'j';
    private static final char UP = 'k';
    private static final char RIGHT = 'l';

    private static final String MOVE_KEYS = "j/k move";
    private static final String PANEL_KEYS = "h/l panel";
    private static final String FOLD_KEY = "enter fold";
    private static final String FOCUS_KEYS = "alt+h/j/k/l focus";
    private static final String QUIT_KEY = "q quit";
    private static final String KEY_BAR_INDENT = " ";
    private static final String KEY_SEPARATOR = "  ";

    private final BoardRows board;
    private Focus focus = Focus.BOARD;
    private Focus lastLeft = Focus.BOARD;
    private Focus lastRight = Focus.MAIN;
    private boolean quitRequested;

    public Shell(BoardView board) {
        this.board = new BoardRows(board);
    }

    @Override
    protected void onStart() {
        runner()
                .eventRouter()
                .addGlobalHandler(
                        event -> event instanceof KeyEvent key && press(key)
                                ? EventResult.HANDLED
                                : EventResult.UNHANDLED);
    }

    Focus focus() {
        return focus;
    }

    boolean quitRequested() {
        return quitRequested;
    }

    boolean press(KeyEvent key) {
        if (key.hasCtrl()) {
            return false;
        }
        if (key.hasAlt()) {
            return pressWithAlt(key);
        }
        if (key.isKey(KeyCode.ENTER)) {
            return focus == Focus.BOARD && board.selectedHeader().isPresent() && fold();
        }
        if (key.isChar(QUIT)) {
            quitRequested = true;
            quit();
            return true;
        }
        if (key.isChar(LEFT)) {
            return focusOn(lastLeft);
        }
        if (key.isChar(RIGHT)) {
            return focusOn(lastRight);
        }
        if (focus == Focus.BOARD && key.isChar(DOWN)) {
            board.down();
            return true;
        }
        if (focus == Focus.BOARD && key.isChar(UP)) {
            board.up();
            return true;
        }
        return false;
    }

    private boolean pressWithAlt(KeyEvent key) {
        if (key.isChar(LEFT)) {
            return focusOn(lastLeft);
        }
        if (key.isChar(RIGHT)) {
            return focusOn(lastRight);
        }
        if (key.isChar(DOWN)) {
            return focusOn(focus.below());
        }
        if (key.isChar(UP)) {
            return focusOn(focus.above());
        }
        return false;
    }

    private boolean fold() {
        board.toggleFold();
        return true;
    }

    private boolean focusOn(Focus target) {
        focus = target;
        if (target.column() == Column.LEFT) {
            lastLeft = target;
        } else {
            lastRight = target;
        }
        return true;
    }

    @Override
    protected Element render() {
        return column(
                text(EMPTY).length(STATUS_LINE_HEIGHT),
                row(
                        column(
                                        boardPanel(),
                                        small(SESSIONS_TITLE, Focus.SESSIONS),
                                        small(DOCS_TITLE, Focus.DOCS),
                                        small(ELSEWHERE_TITLE, Focus.ELSEWHERE))
                                .percent(LEFT_COLUMN_PERCENT),
                        column(mainView(), dock()).fill()).fill(),
                text(keyBar()).dim().length(KEY_BAR_HEIGHT));
    }

    private Element boardPanel() {
        List<String> lines = board.visible().stream().map(Row::text).toList();
        return framed(BOARD_TITLE, Focus.BOARD, new RowsElement(lines, board.selectedIndex(), focus == Focus.BOARD))
                .fill();
    }

    private Element small(String title, Focus owner) {
        return framed(title, owner, text(EMPTY)).length(SMALL_PANEL_HEIGHT);
    }

    private Element mainView() {
        return framed(MAIN_VIEW_TITLE, Focus.MAIN, column(detailLines())).fill();
    }

    private Element dock() {
        return framed(DOCK_TITLE, Focus.DOCK, text(NO_HOSTED_SESSION)).fill();
    }

    private Panel framed(String title, Focus owner, Element body) {
        Panel panel = panel(title, body).rounded();
        return focus == owner ? panel.borderColor(Color.CYAN) : panel;
    }

    private Element[] detailLines() {
        return switch (board.selected()) {
            case HeaderRow header -> new Element[0];
            case TicketRow row -> ticketDetail(row.ticket());
            case UnreadableRow row ->
                new Element[] {text(row.unreadable().file().value()).bold(), text(row.unreadable().reason().value())};
        };
    }

    private static Element[] ticketDetail(TicketView ticket) {
        return new Element[] {
            text(ticket.id().value()).bold(), text(TicketText.title(ticket)), text(TicketText.facts(ticket))
        };
    }

    private String keyBar() {
        List<String> keys = new ArrayList<>();
        if (focus == Focus.BOARD) {
            keys.add(MOVE_KEYS);
        }
        keys.add(PANEL_KEYS);
        if (focus == Focus.BOARD && board.selectedHeader().isPresent()) {
            keys.add(FOLD_KEY);
        }
        keys.add(FOCUS_KEYS);
        keys.add(QUIT_KEY);
        return KEY_BAR_INDENT + String.join(KEY_SEPARATOR, keys);
    }
}
