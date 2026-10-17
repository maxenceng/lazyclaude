package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.richText;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.stack;
import static dev.tamboui.toolkit.Toolkit.text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoicePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchingSessionPayload;
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
import dev.tamboui.terminal.BackendFactory;
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
    private static final String NO_SESSION_ELSEWHERE = "nothing elsewhere";
    private static final String SESSIONS_UNAVAILABLE = "sessions unavailable: ";

    private static final int DEFAULT_PANE_WIDTH = 80;
    private static final int DEFAULT_PANE_HEIGHT = 24;

    private static final char QUIT = 'q';
    private static final char NEW_SESSION = 'N';
    private static final char REFRESH = 'R';
    private static final char NEW_TICKET_SESSION = 'n';
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
    private static final String REFRESH_KEY = "R refresh";
    private static final String NEW_KEY = "N new";
    private static final String NEW_TICKET_KEY = "n new";
    private static final String MAIN_VIEW_KEY = "m main view";
    private static final String SESSION_KEYS = "alt+h/l tab  alt+h/j/k/l focus  alt+m main view";

    private final BoardRows board;
    private final DocsRows docs;
    private final SessionRows sessions;
    private final ElsewhereRows elsewhere;
    private final AtomicReference<DocsView> pendingDocs = new AtomicReference<>();
    private final AtomicReference<SessionsView> pendingSessions = new AtomicReference<>();
    private final AtomicReference<PullRequestsView> pendingPullRequests = new AtomicReference<>();
    private final AtomicReference<BoardView> pendingBoard = new AtomicReference<>();
    private final AtomicReference<WarningView> pendingWarning = new AtomicReference<>();
    private final MainViewScroll mainViewScroll = new MainViewScroll();
    private final HostedSessionCommand hosting;
    private final WatchedSessionCommand watching;
    private final TerminalFocus terminalFocus = new TerminalFocus(this::terminalFocusChanged);
    private WatchedSessionPayload lastWatched = new NoWatchedSessionPayload();
    private final SessionQuery screens;
    private final BoardQuery boards;
    private final DocsQuery docsQuery;
    private final ReviewQuery reviews;
    private final Path root;
    private final DockTabs dock = new DockTabs();
    private Map<HostedSessionId, PaneSize> paneSizes = Map.of();
    private final AtomicBoolean repaintPending = new AtomicBoolean();
    private PullRequestsView pullRequests;
    private volatile BoardView lastRead;
    private volatile Set<String> mergedTicketBranches = Set.of();
    private volatile Set<String> latestMergedHeads = Set.of();
    private volatile List<LinkedTicketId> linkedTicketIds;
    private volatile Set<String> mergedHeads = Set.of();
    private final AtomicReference<Set<String>> pendingMergedHeads = new AtomicReference<>();
    private final Executor background;
    private Optional<VerbPicker> picker = Optional.empty();
    private Optional<Rect> sessionsArea = Optional.empty();
    private boolean mergedRead;
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
            WatchedSessionCommand watching,
            SessionQuery screens,
            BoardQuery boards,
            DocsQuery docsQuery,
            ReviewQuery reviews) {
        this(
                board,
                docs,
                sessions,
                pullRequests,
                hosting,
                watching,
                screens,
                boards,
                docsQuery,
                reviews,
                Executors.newVirtualThreadPerTaskExecutor());
    }

    /** {@code background} runs what must not hold up the frame: adding a worktree, the start of a session in it, and a refresh. */
    Shell(
            BoardView board,
            DocsView docs,
            SessionsView sessions,
            PullRequestsView pullRequests,
            HostedSessionCommand hosting,
            WatchedSessionCommand watching,
            SessionQuery screens,
            BoardQuery boards,
            DocsQuery docsQuery,
            ReviewQuery reviews,
            Executor background) {
        this.background = background;
        this.board = new BoardRows(board);
        this.lastRead = board;
        this.linkedTicketIds = BoardRows.ticketIdsOf(board);
        this.docs = new DocsRows(docs);
        this.sessions = new SessionRows(sessions);
        this.elsewhere = new ElsewhereRows(sessions);
        this.pullRequests = pullRequests;
        this.hosting = hosting;
        this.watching = watching;
        this.screens = screens;
        this.boards = boards;
        this.docsQuery = docsQuery;
        this.reviews = reviews;
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
        if (elsewhere.replace(next) && mainViewOwner == Focus.ELSEWHERE) {
            mainViewScroll.reset();
        }
        refreshOffer();
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
     * The merged pull requests have been fetched again. Called on the fetch's thread: when the merged ticket branches differ from the
     * last ones the board was read with, the board is read again there, so git never runs on the UI thread, and the read board is
     * applied on the UI thread, at the next render or key press, so no row changes under a frame being drawn.
     */
    public void mergedPullRequestsChanged(MergedPullRequestsView view) {
        pendingMergedHeads.set(view.headBranches());
        ToolkitRunner reading = runner();
        if (reading != null) {
            reading.runOnRenderThread(this::applyPendingMergedHeads);
        }
        latestMergedHeads = view.headBranches();
        if (mergedTicketBranches().equals(mergedTicketBranches)) {
            return;
        }
        reread();
    }

    /**
     * Something the board is read from changed: the backlog, a ticket worktree's copy of a ticket, or the refs. Callable from any
     * thread; the board is read again on the caller's, with the branches known merged, and applied on the UI thread.
     */
    public void boardChanged() {
        reread();
    }

    private Set<String> mergedTicketBranches() {
        Set<String> branches = BoardRows.ticketBranchesOf(lastRead);
        return latestMergedHeads.stream().filter(branches::contains).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * One read at a time, so a slow read that started first never lands after a newer one; the merged branches are taken under
     * the same lock, so a read never goes ahead with heads older than the last one applied.
     */
    private synchronized void reread() {
        Set<String> merged = mergedTicketBranches();
        boards.boardReadAgainFor(new BoardPayload(root, merged)).ifPresent(read -> {
            lastRead = read;
            mergedTicketBranches = merged;
            pendingBoard.set(read);
            ToolkitRunner running = runner();
            if (running != null) {
                running.runOnRenderThread(this::applyPendingBoard);
            }
            followTickets(read);
        });
    }

    /** Sessions are linked against the tickets of the board as last read, so a ticket created while running links them. */
    private void followTickets(BoardView read) {
        List<LinkedTicketId> ids = BoardRows.ticketIdsOf(read);
        if (!ids.equals(linkedTicketIds)) {
            linkedTicketIds = ids;
            screens.knowTickets(new SessionPayload(root, ids));
        }
    }

    private void applyPendingMergedHeads() {
        Set<String> heads = pendingMergedHeads.getAndSet(null);
        if (heads == null) {
            return;
        }
        mergedHeads = heads;
        mergedRead = true;
        picker.ifPresent(open -> {
            open.checked();
            open.preselecting(choiceFor(open.ticket()));
        });
        refreshOffer();
    }

    private void applyPendingBoard() {
        BoardView next = pendingBoard.getAndSet(null);
        if (next != null) {
            RowKey before = board.selected().key();
            board.replace(next);
            if (!board.selected().key().equals(before) && mainViewOwner == Focus.BOARD) {
                mainViewScroll.reset();
            }
            showSelectedTicketsSessions();
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

    /** The terminal reports its focus through a backend wrapped to strip the reports TamboUI's parser cannot read. */
    @Override
    protected TuiConfig configure() {
        try {
            return config()
                    .toBuilder()
                    .backend(new FocusReportingBackend(BackendFactory.create(), terminalFocus))
                    .build();
        } catch (IOException e) {
            throw new TerminalUnavailableException(e);
        }
    }

    TerminalFocus terminalFocus() {
        return terminalFocus;
    }

    /** The terminal gained or lost focus, reported on the input thread: the watched session is worked out again on the render thread. */
    private void terminalFocusChanged() {
        ToolkitRunner running = runner();
        if (running != null) {
            running.runOnRenderThread(this::syncWatched);
        }
    }

    /**
     * Tells {@code session} which session is in view, when that changed: the selected dock tab's live session while the dock and the
     * terminal both have focus. It runs a process when it closes a notification, so off the render thread.
     */
    private void syncWatched() {
        WatchedSessionPayload now = inSession() && terminalFocus.focused()
                ? dock.selectedTab()
                        .flatMap(HostedSessionView::live)
                        .<WatchedSessionPayload>map(live -> new WatchingSessionPayload(live.id()))
                        .orElseGet(NoWatchedSessionPayload::new)
                : new NoWatchedSessionPayload();
        if (!now.equals(lastWatched)) {
            lastWatched = now;
            background.execute(() -> watching.watching(now));
        }
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
        applyPendingBoard();
        applyPendingMergedHeads();
        applyPendingWarning();
        if (quitPrompt.isPresent()) {
            return answerQuitPrompt(key);
        }
        if (picker.isPresent()) {
            return pressInPicker(picker.get(), key);
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
        if (key.isChar(REFRESH)) {
            return refresh();
        }
        if (key.isChar(NEW_TICKET_SESSION)) {
            return newFromTicket();
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
        if (focus == Focus.ELSEWHERE && key.isChar(DOWN)) {
            return selectElsewhere(elsewhere::down);
        }
        if (focus == Focus.ELSEWHERE && key.isChar(UP)) {
            return selectElsewhere(elsewhere::up);
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

    /** Reads everything again, off the render thread, and shows only what changed; the pull requests are fetched on their own thread. */
    private boolean refresh() {
        background.execute(() -> {
            reviews.refresh();
            screens.refresh(new SessionPayload(root, linkedTicketIds));
            docsChanged(docsQuery.docsFor(new DocsPayload(root)));
            boardChanged();
        });
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
                sized(List.of(started.id()), paneSize());
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
        List<HostedSessionId> resized = dock.tabs().stream()
                .map(HostedSessionView::id)
                .filter(id -> !size.equals(paneSizes.get(id)))
                .toList();
        resized.forEach(id -> hosting.resize(new ResizePayload(id, size)));
        sized(resized, size);
    }

    /** Remembers that the ptys of {@code ids} now have {@code size}. */
    private void sized(List<HostedSessionId> ids, PaneSize size) {
        paneSizes = Stream.concat(
                        paneSizes.entrySet().stream().filter(sized -> !ids.contains(sized.getKey())),
                        ids.stream().map(id -> Map.entry(id, size)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
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
        refreshOffer();
    }

    private Optional<TicketView> selectedTicket() {
        return board.selected() instanceof TicketRow row ? Optional.of(row.ticket()) : Optional.empty();
    }

    /** Asks {@code session} again which verb the shown ticket's {@code n new} row offers; a ticket with no verb left offers none. */
    private void refreshOffer() {
        sessions.offering(selectedTicket()
                .filter(ticket -> !ticket.verbs().isEmpty())
                .map(ticket -> choiceFor(ticket).preselected()));
    }

    private VerbChoiceView choiceFor(TicketView ticket) {
        return screens.verbChoiceFor(new VerbChoicePayload(
                new SessionPayload(root, BoardRows.ticketIdsOf(lastRead)),
                new LinkedTicketId(ticket.id().value()),
                ticket.verbs(),
                mergedHeads.contains(ticket.branch().value())));
    }

    private boolean newFromTicket() {
        if (focus != Focus.BOARD && focus != Focus.SESSIONS) {
            return false;
        }
        return selectedTicket().map(this::newSessionFor).orElse(false);
    }

    private boolean newSessionFor(TicketView ticket) {
        if (ticket.verbs().isEmpty()) {
            return opened(hosting.startUnlinked(new NewHostedPayload(root, paneSize())));
        }
        picker = Optional.of(new VerbPicker(ticket, choiceFor(ticket), !mergedRead));
        return true;
    }

    private boolean pressInPicker(VerbPicker open, KeyEvent key) {
        if (key.isKey(KeyCode.ESCAPE)) {
            picker = Optional.empty();
        } else if (key.isKey(KeyCode.ENTER)) {
            startVerb(open);
        } else if (key.isChar(DOWN) || key.isKey(KeyCode.DOWN)) {
            open.down();
        } else if (key.isChar(UP) || key.isKey(KeyCode.UP)) {
            open.up();
        }
        return true;
    }

    private void startVerb(VerbPicker open) {
        picker = Optional.empty();
        TicketView ticket = open.ticket();
        TicketHostedPayload payload = new TicketHostedPayload(
                root, new LinkedTicketId(ticket.id().value()), ticket.branch().value(), open.selected(), paneSize());
        inBackground(() -> hosting.startTicket(payload));
    }

    /** Starts off the render thread, since adding a worktree takes git a while, and focuses the tab on the render thread once it is there. */
    private void inBackground(Supplier<HostStart> start) {
        background.execute(() -> {
            HostStart result = start.get();
            ToolkitRunner running = runner();
            if (running == null) {
                opened(result);
            } else {
                running.runOnRenderThread(() -> opened(result));
            }
        });
    }

    private boolean selectDoc(Runnable move) {
        Optional<RowKey> before = docs.selectedKey();
        move.run();
        if (!docs.selectedKey().equals(before)) {
            mainViewScroll.reset();
        }
        return true;
    }

    private boolean selectElsewhere(Runnable move) {
        Optional<RowKey> before = elsewhere.selectedKey();
        move.run();
        if (!elsewhere.selectedKey().equals(before)) {
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

    /**
     * Enter on a hosted session focuses its tab; on a past session it resumes it in a tab of its own, in the ticket's worktree when it is
     * listed under a ticket; on the {@code n new} row it opens the verb picker.
     */
    private boolean enterSession(SessionRow row) {
        return switch (row) {
            case LiveSessionRow live when live.session().isHosted() -> focusTabOf(live.session());
            case PastSessionRow past when past.session().isUnlinked() -> resume(past.session());
            case PastSessionRow past -> selectedTicket().map(ticket -> resumeInTicket(past.session(), ticket)).orElse(false);
            case StartingSessionRow starting -> focusTab(starting.tab().id());
            case NewSessionRow newSession -> selectedTicket().map(this::newSessionFor).orElse(false);
            default -> false;
        };
    }

    private boolean focusTab(HostedSessionId id) {
        dock.select(id);
        return focusOn(Focus.DOCK);
    }

    private boolean resumeInTicket(PastSessionView session, TicketView ticket) {
        ResumeTicketHostedPayload payload =
                new ResumeTicketHostedPayload(
                        session.id(), session.directory(), root, ticket.branch().value(), paneSize());
        inBackground(() -> hosting.resumeInTicketWorktree(payload));
        return true;
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
        if (target.column() == Column.LEFT && target != mainViewOwner) {
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
        applyPendingBoard();
        applyPendingMergedHeads();
        applyPendingWarning();
        syncWatched();
        Element screen = column(
                new StatusLineElement(
                        StatusText.spans(sessions.markerCounts(), elsewhere.groups()),
                        warning,
                        quitPrompt.map(SessionText::quitPrompt)),
                row(
                        column(
                                        boardPanel(),
                                        sessionsPanel(),
                                        docsPanel(),
                                        elsewherePanel())
                                .percent(LEFT_COLUMN_PERCENT),
                        (mainViewHidden ? column(dock()) : column(mainView(), dock())).fill()).fill(),
                new KeyBarElement(keyBar()));
        return picker.<Element>map(open -> stack(screen, new VerbPickerElement(open, () -> sessionsArea)))
                .orElse(screen);
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
        return sized(
                framed(SESSIONS_TITLE, Focus.SESSIONS, new PaneProbe(body, area -> sessionsArea = Optional.of(area))),
                Focus.SESSIONS);
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

    private Element elsewherePanel() {
        Element body = elsewhere.isEmpty()
                ? text(NO_SESSION_ELSEWHERE).dim()
                : new RowsElement(
                        elsewhere.visible().stream().map(ElsewhereRow::line).toList(),
                        elsewhere.selectedIndex(),
                        focus == Focus.ELSEWHERE);
        Panel panel = framed(ELSEWHERE_TITLE, Focus.ELSEWHERE, body);
        return mainViewOwner == Focus.ELSEWHERE ? panel.fill() : panel.length(SMALL_PANEL_HEIGHT);
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
            case ELSEWHERE -> elsewhereDetail();
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
                    case NewSessionRow newSession -> column();
                    case StartingSessionRow starting -> column();
                })
                .orElseGet(() -> column());
    }

    private Element elsewhereDetail() {
        return elsewhere
                .selected()
                .<Element>map(row -> new DetailElement(
                        width -> SessionText.elsewhereFacts(row.place(), row.session()), Optional.empty(), mainViewScroll))
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

    private List<String> keyBar() {
        if (picker.isPresent()) {
            return List.of(VerbPickerText.keys());
        }
        if (inSession()) {
            return List.of(SESSION_KEYS);
        }
        return Stream.of(
                        Included.when(focus.column() == Column.LEFT, MOVE_KEYS),
                        Included.when(focus == Focus.MAIN, SCROLL_KEYS),
                        Stream.of(PANEL_KEYS),
                        Included.when(foldable(), FOLD_KEY),
                        Included.when(focus == Focus.BOARD && board.selected() instanceof TicketRow, SESSIONS_KEY),
                        Included.when(focus == Focus.SESSIONS && sessions.olderSelected(), EXPAND_KEY),
                        Stream.of(FOCUS_KEYS, QUIT_KEY),
                        Included.when(
                                (focus == Focus.BOARD || focus == Focus.SESSIONS) && selectedTicket().isPresent(),
                                NEW_TICKET_KEY),
                        Stream.of(NEW_KEY, REFRESH_KEY, MAIN_VIEW_KEY))
                .flatMap(Function.identity())
                .toList();
    }

    private boolean foldable() {
        return focus == Focus.BOARD && board.selectedHeader().isPresent()
                || focus == Focus.DOCS && docs.selectedFolder().isPresent();
    }
}
