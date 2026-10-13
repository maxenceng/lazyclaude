package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.richText;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.HostedSessionId;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PaneHeight;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.PaneWidth;
import dev.maxenceng.lazyclaude.session.published.PastedText;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

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

    private static final int DEFAULT_PANE_WIDTH = 80;
    private static final int DEFAULT_PANE_HEIGHT = 24;

    private static final char QUIT = 'q';
    private static final char NEW_SESSION = 'N';
    private static final char TOGGLE_MAIN_VIEW = 'm';
    private static final char SUSPEND = 'z';
    private static final char NEXT_WAITING = 'w';
    private static final char YES = 'y';
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
    private static final String NEW_KEY = "N new";
    private static final String MAIN_VIEW_KEY = "m main view";
    private static final String SESSION_KEYS = "alt+h/l tab  alt+h/j/k/l focus  alt+m main view";
    private static final String KEY_BAR_INDENT = " ";
    private static final String KEY_SEPARATOR = "  ";

    private final BoardRows board;
    private final DocsRows docs;
    private final SessionRows sessions;
    private final AtomicReference<DocsView> pendingDocs = new AtomicReference<>();
    private final AtomicReference<SessionsView> pendingSessions = new AtomicReference<>();
    private final AtomicReference<PullRequestsView> pendingPullRequests = new AtomicReference<>();
    private final AtomicReference<WarningView> pendingWarning = new AtomicReference<>();
    private final MainViewScroll mainViewScroll = new MainViewScroll();
    private final HostedSessionCommand hosting;
    private final SessionQuery screens;
    private final Path root;
    private final DockTabs dock = new DockTabs();
    private final Map<HostedSessionId, PaneSize> paneSizes = new HashMap<>();
    private final AtomicBoolean repaintPending = new AtomicBoolean();
    private PullRequestsView pullRequests;
    private Optional<Rect> paneArea = Optional.empty();
    private Optional<Long> quitPrompt = Optional.empty();
    private boolean mainViewHidden;
    private Focus focus = Focus.BOARD;
    private Focus lastLeft = Focus.BOARD;
    private Focus lastRight = Focus.MAIN;
    private Focus mainViewOwner = Focus.BOARD;
    private Optional<WarningView> warning = Optional.empty();
    private boolean quitRequested;

    public Shell(
            BoardView board,
            DocsView docs,
            SessionsView sessions,
            PullRequestsView pullRequests,
            HostedSessionCommand hosting,
            SessionQuery screens) {
        this.board = new BoardRows(board);
        this.docs = new DocsRows(docs);
        this.sessions = new SessionRows(sessions);
        this.pullRequests = pullRequests;
        this.hosting = hosting;
        this.screens = screens;
        this.root = board.root().value();
        this.dock.replace(sessions.hosted());
    }

    /** A hosted session drew something. Callable from any thread; the next frame shows it, and this makes sure one is drawn soon. */
    public void screensChanged() {
        ToolkitRunner running = runner();
        if (running != null && repaintPending.compareAndSet(false, true)) {
            running.runOnRenderThread(() -> repaintPending.set(false));
        }
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
        if (next == null) {
            return;
        }
        if (sessions.replace(next) && mainViewOwner == Focus.SESSIONS) {
            mainViewScroll.reset();
        }
        TabClosing closing = dock.replace(sessions.hosted());
        if (focus == Focus.DOCK && closing == TabClosing.LAST_ONE_GONE) {
            focusOn(lastLeft);
        }
    }

    /**
     * The open pull requests have been fetched again: the open ones, or why they could not be read. Callable from any thread: they are
     * applied on the UI thread, at the next render or key press, so no glyph changes under a frame being drawn.
     */
    public void pullRequestsChanged(PullRequestsView view) {
        pendingPullRequests.set(view);
        ToolkitRunner running = runner();
        if (running != null) {
            running.runOnRenderThread(this::applyPendingPullRequests);
        }
    }

    private void applyPendingPullRequests() {
        PullRequestsView next = pendingPullRequests.getAndSet(null);
        if (next != null) {
            pullRequests = next;
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
    protected TuiConfig configure() {
        return config();
    }

    /**
     * Bracketed paste and mouse capture are on, so a paste arrives whole and the wheel and clicks reach a session; Tab and
     * Shift+Tab are unbound from focus, so they reach it too. Quit is unbound, since the shell decides when {@code q} and Ctrl+C
     * may quit.
     */
    TuiConfig config() {
        return TuiConfig.defaults()
                .toBuilder()
                .bracketedPaste(true)
                .mouseCapture(true)
                .bindings(BindingSets.defaults()
                        .toBuilder()
                        .unbind(Actions.FOCUS_NEXT)
                        .unbind(Actions.FOCUS_PREVIOUS)
                        .unbind(Actions.QUIT)
                        .build())
                .build();
    }

    @Override
    protected void onStart() {
        runner().eventRouter().addGlobalHandler(event -> handled(event) ? EventResult.HANDLED : EventResult.UNHANDLED);
    }

    private boolean handled(Event event) {
        return switch (event) {
            case KeyEvent key -> press(key);
            case MouseEvent mouse -> mouse(mouse);
            default -> false;
        };
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
        applyPendingPullRequests();
        applyPendingWarning();
        if (quitPrompt.isPresent()) {
            return answerQuitPrompt(key);
        }
        if (inSession()) {
            return pressInSession(key);
        }
        if (key.hasCtrl()) {
            return key.isCtrlC() ? requestQuit() : focus == Focus.MAIN && pressWithCtrl(key);
        }
        if (key.hasAlt()) {
            return pressWithAlt(key);
        }
        if (key.isKey(KeyCode.ENTER)) {
            return fold();
        }
        if (key.isChar(QUIT)) {
            return requestQuit();
        }
        if (key.isChar(NEW_SESSION)) {
            return opened(hosting.startUnlinked(new NewHostedPayload(root, paneSize())));
        }
        if (key.isChar(TOGGLE_MAIN_VIEW)) {
            return toggleMainView();
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

    /** Whether a hosted session's tab has focus, so that every key but the reserved chords goes to its {@code claude}. */
    private boolean inSession() {
        return focus == Focus.DOCK && dock.selectedTab().isPresent();
    }

    private boolean pressInSession(KeyEvent key) {
        if (key.hasAlt() && !key.hasCtrl() && pressReservedChord(key)) {
            return true;
        }
        if (key.hasCtrl() && key.isChar(SUSPEND)) {
            return true;
        }
        TerminalInputs.key(key).ifPresent(this::sendToSelectedTab);
        return true;
    }

    private boolean pressReservedChord(KeyEvent key) {
        if (key.isChar(LEFT)) {
            return dock.selectPrevious() || focusOn(lastLeft);
        }
        if (key.isChar(RIGHT)) {
            dock.selectNext();
            return true;
        }
        if (key.isChar(UP)) {
            return goUp(Focus.MAIN);
        }
        if (key.isChar(TOGGLE_MAIN_VIEW)) {
            return toggleMainView();
        }
        return key.isChar(DOWN) || key.isChar(SUSPEND) || key.isChar(NEXT_WAITING);
    }

    private void sendToSelectedTab(TerminalInput input) {
        dock.selectedTab().ifPresent(tab -> hosting.send(new HostedInputPayload(tab.id(), input)));
    }

    /** A paste into the pane, handed to the session whose tab has focus and dropped otherwise. */
    void paste(PastedText paste) {
        if (inSession()) {
            sendToSelectedTab(paste);
        }
    }

    /** A mouse event over the pane reaches the selected tab's session at the matching cell, whatever has focus. */
    boolean mouse(MouseEvent event) {
        Optional<HostedSessionView> tab = dock.selectedTab();
        if (tab.isEmpty() || paneArea.isEmpty()) {
            return false;
        }
        return TerminalInputs.mouse(event, paneArea.get())
                .map(input -> {
                    hosting.send(new HostedInputPayload(tab.get().id(), input));
                    return true;
                })
                .orElse(false);
    }

    private boolean requestQuit() {
        long busy = sessions.busyOrWaitingHosted();
        if (busy == 0) {
            quitRequested = true;
            quit();
        } else {
            quitPrompt = Optional.of(busy);
        }
        return true;
    }

    private boolean answerQuitPrompt(KeyEvent key) {
        quitPrompt = Optional.empty();
        if (key.isChar(YES) && !key.hasCtrl() && !key.hasAlt()) {
            quitRequested = true;
            quit();
        }
        return true;
    }

    private boolean toggleMainView() {
        mainViewHidden = !mainViewHidden;
        if (mainViewHidden && focus == Focus.MAIN) {
            focusOn(Focus.DOCK);
        }
        return true;
    }

    /** Focuses the tab of a session just started, or does nothing when none was. */
    private boolean opened(HostStart start) {
        applyPendingSessions();
        switch (start) {
            case StartedHost started -> {
                dock.select(started.id());
                paneSizes.put(started.id(), paneSize());
                focusOn(Focus.DOCK);
            }
            case NotStartedHost none -> {}
        }
        return true;
    }

    private PaneSize paneSize() {
        return paneArea
                .map(area -> PaneSize.builder()
                        .width(new PaneWidth(area.width()))
                        .height(new PaneHeight(area.height()))
                        .build())
                .orElseGet(() -> PaneSize.builder()
                        .width(new PaneWidth(DEFAULT_PANE_WIDTH))
                        .height(new PaneHeight(DEFAULT_PANE_HEIGHT))
                        .build());
    }

    /** The dock was drawn in {@code area}: the pty of every tab takes that size, once, and again whenever it changes. */
    private void paneDrawn(Rect area) {
        paneArea = Optional.of(area);
        PaneSize size = paneSize();
        dock.tabs().stream()
                .map(HostedSessionView::id)
                .filter(id -> !size.equals(paneSizes.get(id)))
                .forEach(id -> {
                    hosting.resize(new ResizePayload(id, size));
                    paneSizes.put(id, size);
                });
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
            return goUp(focus.above());
        }
        if (key.isChar(TOGGLE_MAIN_VIEW)) {
            return toggleMainView();
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
        if (focus == Focus.SESSIONS) {
            return sessions.selected().map(this::enterSession).orElse(false);
        }
        return false;
    }

    /** Enter on a hosted session focuses its tab; on an unlinked past session it resumes it in a tab of its own. */
    private boolean enterSession(SessionRow row) {
        return switch (row) {
            case LiveSessionRow live when live.session().isHosted() -> focusTabOf(live.session());
            case PastSessionRow past when past.session().isUnlinked() -> resume(past.session());
            default -> false;
        };
    }

    private boolean focusTabOf(LiveSessionView session) {
        dock.tabs().stream()
                .filter(tab -> tab.live().map(LiveSessionView::id).filter(session.id()::equals).isPresent())
                .findFirst()
                .ifPresent(tab -> {
                    dock.select(tab.id());
                    focusOn(Focus.DOCK);
                });
        return true;
    }

    private boolean resume(PastSessionView session) {
        return opened(hosting.resume(new ResumeHostedPayload(session.id(), session.directory(), paneSize())));
    }

    private boolean goUp(Focus above) {
        if (above == Focus.MAIN) {
            mainViewHidden = false;
        }
        return focusOn(above);
    }

    private boolean focusOn(Focus requested) {
        Focus target = requested == Focus.MAIN && mainViewHidden ? Focus.DOCK : requested;
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
        applyPendingPullRequests();
        applyPendingWarning();
        return column(
                new StatusLineElement(sessions.strongestMarker(), warning, quitPrompt.map(SessionText::quitPrompt)),
                row(
                        column(
                                        boardPanel(),
                                        sessionsPanel(),
                                        docsPanel(),
                                        small(ELSEWHERE_TITLE, Focus.ELSEWHERE))
                                .percent(LEFT_COLUMN_PERCENT),
                        (mainViewHidden ? column(dock()) : column(mainView(), dock())).fill()).fill(),
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
                ? ticket.line(
                        sessions.markerOf(new LinkedTicketId(ticket.ticket().id().value())),
                        PullRequestText.column(ticket.ticket(), pullRequests))
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
        Element body = dock.selectedTab()
                .<Element>map(tab -> new TerminalScreenElement(screens.screenOf(tab.id()), focus == Focus.DOCK, this::paste))
                .orElseGet(() -> text(NO_HOSTED_SESSION));
        Panel panel = panel(DOCK_TITLE, new PaneProbe(body, this::paneDrawn))
                .title(DockText.title(dock.tabs(), dock.selectedIndex()))
                .rounded()
                .titleEllipsis();
        return (focus == Focus.DOCK ? panel.borderColor(Color.CYAN) : panel).fill();
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
        return new DetailElement(width -> TicketText.head(ticket, pullRequests, width), TicketText.markdown(ticket), mainViewScroll);
    }

    private String keyBar() {
        if (inSession()) {
            return KEY_BAR_INDENT + SESSION_KEYS;
        }
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
        keys.add(NEW_KEY);
        keys.add(MAIN_VIEW_KEY);
        return KEY_BAR_INDENT + String.join(KEY_SEPARATOR, keys);
    }

    private boolean foldable() {
        return focus == Focus.BOARD && board.selectedHeader().isPresent()
                || focus == Focus.DOCS && docs.selectedFolder().isPresent();
    }
}
