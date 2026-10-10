package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * The terminal application itself. It owns the screen and the keyboard, and composes what
 * each context's application layer offers; it holds no rules of its own.
 */
public final class Shell extends ToolkitApp {

    private static final int LEFT_COLUMN_PERCENT = 35;
    private static final int SMALL_PANEL_HEIGHT = 3;

    private final BoardRows board;
    private Focus focus = Focus.BOARD;
    private Focus lastLeft = Focus.BOARD;
    private Focus lastRight = Focus.MAIN;
    private boolean quitRequested;

    public Shell(Board board) {
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
        if (key.isChar('q')) {
            quitRequested = true;
            quit();
            return true;
        }
        if (key.isChar('h')) {
            return moveFocusLeft();
        }
        if (key.isChar('l')) {
            return moveFocusRight();
        }
        if (focus == Focus.BOARD && key.isChar('j')) {
            board.down();
            return true;
        }
        if (focus == Focus.BOARD && key.isChar('k')) {
            board.up();
            return true;
        }
        return false;
    }

    private boolean pressWithAlt(KeyEvent key) {
        if (key.isChar('h')) {
            return moveFocusLeft();
        }
        if (key.isChar('l')) {
            return moveFocusRight();
        }
        if (key.isChar('j')) {
            return focusOn(focus.below());
        }
        if (key.isChar('k')) {
            return focusOn(focus.above());
        }
        return false;
    }

    private boolean fold() {
        board.toggleFold();
        return true;
    }

    private boolean moveFocusLeft() {
        return focusOn(lastLeft);
    }

    private boolean moveFocusRight() {
        return focusOn(lastRight);
    }

    private boolean focusOn(Focus target) {
        focus = target;
        if (target.column() == Focus.Column.LEFT) {
            lastLeft = target;
        } else {
            lastRight = target;
        }
        return true;
    }

    @Override
    protected Element render() {
        return column(
                text("").length(1),
                row(
                        column(
                                        boardPanel(),
                                        small("Sessions", Focus.SESSIONS),
                                        small("Docs", Focus.DOCS),
                                        small("Elsewhere", Focus.ELSEWHERE))
                                .percent(LEFT_COLUMN_PERCENT),
                        column(mainView(), dock()).fill()).fill(),
                text(keyBar()).dim().length(1));
    }

    private Element boardPanel() {
        List<BoardRows.Row> rows = board.visible();
        List<String> lines = new ArrayList<>();
        for (BoardRows.Row row : rows) {
            lines.add(switch (row) {
                case BoardRows.Row.Header header -> header.text();
                case BoardRows.Row.TicketRow ticket -> ticket.text();
                case BoardRows.Row.UnreadableRow unreadable -> unreadable.text();
            });
        }
        return framed("Board", Focus.BOARD, new RowsElement(lines, board.selectedIndex(), focus == Focus.BOARD))
                .fill();
    }

    private Element small(String title, Focus owner) {
        return framed(title, owner, text("")).length(SMALL_PANEL_HEIGHT);
    }

    private Element mainView() {
        return framed("main view", Focus.MAIN, column(detailLines())).fill();
    }

    private Element dock() {
        return framed("dock", Focus.DOCK, text("no hosted session")).fill();
    }

    private Panel framed(String title, Focus owner, Element body) {
        Panel panel = panel(title, body).rounded();
        return focus == owner ? panel.borderColor(Color.CYAN) : panel;
    }

    private Element[] detailLines() {
        return switch (board.selected()) {
            case BoardRows.Row.Header header -> new Element[0];
            case BoardRows.Row.TicketRow row -> {
                var ticket = row.ticket();
                List<String> parts = new ArrayList<>(List.of("status " + ticket.status().label()));
                if (!ticket.context().isBlank()) {
                    parts.add(ticket.context());
                }
                if (!ticket.type().isBlank()) {
                    parts.add(ticket.type());
                }
                yield new Element[] {
                    text(ticket.id().value()).bold(), text(ticket.title()), text(String.join(" · ", parts))
                };
            }
            case BoardRows.Row.UnreadableRow row ->
                new Element[] {text(row.unreadable().file()).bold(), text(row.unreadable().reason())};
        };
    }

    private String keyBar() {
        List<String> keys = new ArrayList<>();
        if (focus == Focus.BOARD) {
            keys.add("j/k move");
        }
        keys.add("h/l panel");
        if (focus == Focus.BOARD && board.selectedHeader().isPresent()) {
            keys.add("enter fold");
        }
        keys.add("alt+h/j/k/l focus");
        keys.add("q quit");
        return " " + String.join("  ", keys);
    }
}
