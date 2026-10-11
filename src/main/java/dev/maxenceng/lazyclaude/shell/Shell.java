package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.richText;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
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
    private static final int SHRUNK_PANEL_HEIGHT = 5;
    private static final int KEY_BAR_HEIGHT = 1;

    private static final String BOARD_TITLE = "Board";
    private static final String SESSIONS_TITLE = "Sessions";
    private static final String DOCS_TITLE = "Docs";
    private static final String ELSEWHERE_TITLE = "Elsewhere";
    private static final String MAIN_VIEW_TITLE = "main view";
    private static final String DOCK_TITLE = "dock";
    private static final String NO_HOSTED_SESSION = "no hosted session";
    private static final String NO_DOCS = "no docs";
    private static final String DOCS_UNAVAILABLE = "docs unavailable";
    private static final String NO_LIVE_SESSION = "no live session";
    private static final String SESSIONS_UNAVAILABLE = "sessions unavailable: ";
    private static final String EMPTY = "";

    private static final char QUIT = 'q';
    private static final char LEFT = 'h';
    private static final char DOWN = 'j';
    private static final char UP = 'k';
    private static final char RIGHT = 'l';
    private static final char TOP = 'g';
    private static final char BOTTOM = 'G';
    private static final char HALF_PAGE_DOWN = 'd';
    private static final char HALF_PAGE_UP = 'u';

    private static final String MOVE_KEYS = "j/k move";
    private static final String SCROLL_KEYS = "j/k scroll  ^d/^u half page  g/G top/bottom";
    private static final String PANEL_KEYS = "h/l panel";
    private static final String FOLD_KEY = "enter fold";
    private static final String SESSIONS_KEY = "enter sessions";
    private static final String EXPAND_KEY = "enter expand";
    private static final String FOCUS_KEYS = "alt+h/j/k/l focus";
    private static final String QUIT_KEY = "q quit";
    private static final String KEY_BAR_INDENT = " ";
    private static final String KEY_SEPARATOR = "  ";

    private final BoardRows board;
    private final DocsRows docs;
    private final SessionRows sessions;
    private final AtomicReference<DocsView> pendingDocs = new AtomicReference<>();
    private final AtomicReference<SessionsView> pendingSessions = new AtomicReference<>();
    private final AtomicReference<WarningView> pendingWarning = new AtomicReference<>();
    private final MainViewScroll mainViewScroll = new MainViewScroll();
    private Focus focus = Focus.BOARD;
    private Focus lastLeft = Focus.BOARD;
    private Focus lastRight = Focus.MAIN;
    private Focus mainViewOwner = Focus.BOARD;
    private Optional<WarningView> warning = Optional.empty();
    private boolean quitRequested;

    public Shell(BoardView board, DocsView docs, SessionsView sessions) {
        this.board = new BoardRows(board);
        this.docs = new DocsRows(docs);
        this.sessions = new SessionRows(sessions);
    }

    /**
     * The docs have been read again: a new tree, or why there is none. Callable from any thread: the tree is applied on the UI thread,
     * at the next render or key press, so no row changes under a frame being drawn.
     */
    public void docsChanged(DocsView view) {
        pendingDocs.set(view);
        ToolkitRunner running = runner();
        if (running != null) {
            running.runOnRenderThread(this::applyPendingDocs);
        }
    }

    private void applyPendingDocs() {
        DocsView next = pendingDocs.getAndSet(null);
        if (next != null && docs.replace(next) && mainViewOwner == Focus.DOCS) {
            mainViewScroll.reset();
        }
    }

    /**
     * The sessions have been read again: new ones, or why there are none. Callable from any thread: they are applied on the UI
     * thread, at the next render or key press, so no row changes under a frame being drawn.
     */
    public void sessionsChanged(SessionsView view) {
        pendingSessions.set(view);
        ToolkitRunner running = runner();
        if (running != null) {
            running.runOnRenderThread(this::applyPendingSessions);
        }
    }

    private void applyPendingSessions() {
        SessionsView next = pendingSessions.getAndSet(null);
        if (next != null && sessions.replace(next) && mainViewOwner == Focus.SESSIONS) {
            mainViewScroll.reset();
        }
    }

    /**
     * A warning was raised, or held ones are handed over at start. Callable from any thread: it is applied on the UI thread,
     * at the next render or key press, so the status line never changes under a frame being drawn.
     */
    public void warningsChanged(WarningView view) {
        pendingWarning.set(view);
        ToolkitRunner running = runner();
        if (running != null) {
            running.runOnRenderThread(this::applyPendingWarning);
        }
    }

    private void applyPendingWarning() {
        WarningView next = pendingWarning.getAndSet(null);
        if (next != null) {
            warning = Optional.of(next);
        }
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
        applyPendingDocs();
        applyPendingSessions();
        applyPendingWarning();
        if (key.hasCtrl()) {
            return focus == Focus.MAIN && pressWithCtrl(key);
        }
        if (key.hasAlt()) {
            return pressWithAlt(key);
        }
        if (key.isKey(KeyCode.ENTER)) {
            return fold();
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
            return select(board::down);
        }
        if (focus == Focus.BOARD && key.isChar(UP)) {
            return select(board::up);
        }
        if (focus == Focus.DOCS && key.isChar(DOWN)) {
            return selectDoc(docs::down);
        }
        if (focus == Focus.DOCS && key.isChar(UP)) {
            return selectDoc(docs::up);
        }
        if (focus == Focus.SESSIONS && key.isChar(DOWN)) {
            return selectSession(sessions::down);
        }
        if (focus == Focus.SESSIONS && key.isChar(UP)) {
            return selectSession(sessions::up);
        }
        return focus == Focus.MAIN && scrollMainView(key);
    }

    private boolean pressWithCtrl(KeyEvent key) {
        if (key.isChar(HALF_PAGE_DOWN)) {
            mainViewScroll.halfPageDown();
            return true;
        }
        if (key.isChar(HALF_PAGE_UP)) {
            mainViewScroll.halfPageUp();
            return true;
        }
        return false;
    }

    private boolean scrollMainView(KeyEvent key) {
        if (key.isChar(DOWN)) {
            mainViewScroll.rowDown();
        } else if (key.isChar(UP)) {
            mainViewScroll.rowUp();
        } else if (key.isChar(TOP)) {
            mainViewScroll.top();
        } else if (key.isChar(BOTTOM)) {
            mainViewScroll.bottom();
        } else {
            return false;
        }
        return true;
    }

    private boolean select(Runnable move) {
        RowKey before = board.selected().key();
        move.run();
        if (!board.selected().key().equals(before)) {
            mainViewScroll.reset();
            showSelectedTicketsSessions();
        }
        return true;
    }

    private void showSelectedTicketsSessions() {
        Optional<LinkedTicketId> ticket = board.selected() instanceof TicketRow row
                ? Optional.of(new LinkedTicketId(row.ticket().id().value()))
                : Optional.empty();
        sessions.showTicket(ticket);
    }

    private boolean selectDoc(Runnable move) {
        Optional<RowKey> before = docs.selectedKey();
        move.run();
        if (!docs.selectedKey().equals(before)) {
            mainViewScroll.reset();
        }
        return true;
    }

    private boolean selectSession(Runnable move) {
        Optional<RowKey> before = sessions.selectedKey();
        move.run();
        if (!sessions.selectedKey().equals(before)) {
            mainViewScroll.reset();
        }
        return true;
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
        if (focus == Focus.BOARD && board.selectedHeader().isPresent()) {
            board.toggleFold();
            return true;
        }
        if (focus == Focus.BOARD && board.selected() instanceof TicketRow) {
            sessions.selectStrongestMarker();
            return focusOn(Focus.SESSIONS);
        }
        if (focus == Focus.DOCS && docs.selectedFolder().isPresent()) {
            docs.toggleFold();
            return true;
        }
        if (focus == Focus.SESSIONS && sessions.olderSelected()) {
            sessions.expand();
            mainViewScroll.reset();
            return true;
        }
        return false;
    }

    private boolean focusOn(Focus target) {
        focus = target;
        if (target.column() == Column.LEFT) {
            lastLeft = target;
        } else {
            lastRight = target;
        }
        if ((target == Focus.BOARD || target == Focus.SESSIONS || target == Focus.DOCS) && target != mainViewOwner) {
            mainViewOwner = target;
            mainViewScroll.reset();
        }
        return true;
    }

    @Override
    protected Element render() {
        applyPendingDocs();
        applyPendingSessions();
        applyPendingWarning();
        return column(
                new StatusLineElement(sessions.strongestMarker(), warning),
                row(
                        column(
                                        boardPanel(),
                                        sessionsPanel(),
                                        docsPanel(),
                                        small(ELSEWHERE_TITLE, Focus.ELSEWHERE))
                                .percent(LEFT_COLUMN_PERCENT),
                        column(mainView(), dock()).fill()).fill(),
                text(keyBar()).dim().length(KEY_BAR_HEIGHT));
    }

    private Element boardPanel() {
        List<RowLine> lines = board.visible().stream().map(this::boardLine).toList();
        return sized(
                framed(BOARD_TITLE, Focus.BOARD, new RowsElement(lines, board.selectedIndex(), focus == Focus.BOARD)),
                Focus.BOARD);
    }

    private RowLine boardLine(Row row) {
        return row instanceof TicketRow ticket
                ? ticket.line(sessions.markerOf(new LinkedTicketId(ticket.ticket().id().value())))
                : RowLine.plain(row.text());
    }

    private Element sessionsPanel() {
        Element body = sessions.unavailable()
                .<Element>map(reason -> richText(SESSIONS_UNAVAILABLE + reason.value()).wrapWord().dim())
                .orElseGet(() -> sessions.isEmpty()
                        ? text(NO_LIVE_SESSION).dim()
                        : new SessionRowsElement(sessions.visible(), sessions.selectedIndex(), focus == Focus.SESSIONS));
        return sized(framed(SESSIONS_TITLE, Focus.SESSIONS, body), Focus.SESSIONS);
    }

    private Element docsPanel() {
        List<RowLine> lines = docs.visible().stream().map(row -> RowLine.plain(row.text())).toList();
        Element body = docs.unavailable()
                .<Element>map(reason -> column(text(DOCS_UNAVAILABLE).dim(), text(reason.value()).dim()))
                .orElseGet(() -> docs.isEmpty()
                        ? text(NO_DOCS).dim()
                        : new RowsElement(lines, docs.selectedIndex(), focus == Focus.DOCS));
        return sized(framed(DOCS_TITLE, Focus.DOCS, body), Focus.DOCS);
    }

    private Element sized(Panel panel, Focus owner) {
        return mainViewOwner == owner ? panel.fill() : panel.length(SHRUNK_PANEL_HEIGHT);
    }

    private Element small(String title, Focus owner) {
        return framed(title, owner, text(EMPTY)).length(SMALL_PANEL_HEIGHT);
    }

    private Element mainView() {
        return framed(MAIN_VIEW_TITLE, Focus.MAIN, detail()).fill();
    }

    private Element dock() {
        return framed(DOCK_TITLE, Focus.DOCK, text(NO_HOSTED_SESSION)).fill();
    }

    private Panel framed(String title, Focus owner, Element body) {
        Panel panel = panel(title, body).rounded();
        return focus == owner ? panel.borderColor(Color.CYAN) : panel;
    }

    private Element detail() {
        return switch (mainViewOwner) {
            case BOARD -> boardDetail();
            case SESSIONS -> sessionsDetail();
            default -> docsDetail();
        };
    }

    private Element sessionsDetail() {
        return sessions.selected()
                .map(row -> switch (row) {
                    case LiveSessionRow live ->
                        new DetailElement(width -> SessionText.facts(live.session()), Optional.empty(), mainViewScroll);
                    case PastSessionRow past ->
                        new DetailElement(width -> SessionText.pastFacts(past.session()), Optional.empty(), mainViewScroll);
                    case OlderSessionsRow older -> column();
                    case SectionHeaderRow header -> column();
                    case NoSessionRow none -> column();
                })
                .orElseGet(() -> column());
    }

    private Element docsDetail() {
        return docs.selected()
                .map(row -> switch (row) {
                    case FolderRow folder -> column();
                    case DocumentRow document -> documentDetail(document.document());
                })
                .orElseGet(() -> column());
    }

    private Element documentDetail(DocumentView document) {
        return new DetailElement(width -> DocsText.head(document), DocsText.markdown(document), mainViewScroll);
    }

    private Element boardDetail() {
        return switch (board.selected()) {
            case HeaderRow header -> column();
            case TicketRow row -> ticketDetail(row.ticket());
            case UnreadableRow row ->
                column(text(row.unreadable().file().value()).bold(), text(row.unreadable().reason().value()));
        };
    }

    private Element ticketDetail(TicketView ticket) {
        return new DetailElement(width -> TicketText.head(ticket, width), TicketText.markdown(ticket), mainViewScroll);
    }

    private String keyBar() {
        List<String> keys = new ArrayList<>();
        if (focus == Focus.BOARD || focus == Focus.SESSIONS || focus == Focus.DOCS) {
            keys.add(MOVE_KEYS);
        }
        if (focus == Focus.MAIN) {
            keys.add(SCROLL_KEYS);
        }
        keys.add(PANEL_KEYS);
        if (foldable()) {
            keys.add(FOLD_KEY);
        }
        if (focus == Focus.BOARD && board.selected() instanceof TicketRow) {
            keys.add(SESSIONS_KEY);
        }
        if (focus == Focus.SESSIONS && sessions.olderSelected()) {
            keys.add(EXPAND_KEY);
        }
        keys.add(FOCUS_KEYS);
        keys.add(QUIT_KEY);
        return KEY_BAR_INDENT + String.join(KEY_SEPARATOR, keys);
    }

    private boolean foldable() {
        return focus == Focus.BOARD && board.selectedHeader().isPresent()
                || focus == Focus.DOCS && docs.selectedFolder().isPresent();
    }
}
