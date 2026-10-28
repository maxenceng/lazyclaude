package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.stack;
import static dev.tamboui.toolkit.Toolkit.text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardPayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsPayload;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsView;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocumentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HeldWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RefusedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RemovedWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResizePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ResumeTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketHostedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoicePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;
import dev.maxenceng.lazyclaude.session.published.FixedChord;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PaneHeight;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.PaneWidth;
import dev.maxenceng.lazyclaude.session.published.PastedText;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.maxenceng.lazyclaude.session.published.TerminalInput;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.toolkit.Toolkit;
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
    private static final int SHRUNK_PANEL_HEIGHT = 5;

    private static final String RESPOND_TITLE = "Respond";
    private static final String RESPOND_PROMPT = "start /ticket respond %s?";
    private static final String RESPOND_HINT = "y start  esc back";
    private static final String BOARD_TITLE = "Board";
    private static final String SESSIONS_TITLE = "Sessions";
    private static final String DOCS_TITLE = "Docs";
    private static final String MAIN_VIEW_TITLE = "main view";
    private static final String DOCK_TITLE = "dock";
    private static final String NO_HOSTED_SESSION = "no hosted session";
    private static final String NO_DOCS = "no docs";
    private static final String DOCS_UNAVAILABLE = "docs unavailable";
    private static final String NO_LIVE_SESSION = "no live session";

    private static final int DEFAULT_PANE_WIDTH = 80;
    private static final int DEFAULT_PANE_HEIGHT = 24;

    private static final char QUIT = 'q';
    private static final char NEW_SESSION = 'N';
    private static final char REFRESH = 'R';
    private static final char NEW_TICKET_SESSION = 'n';
    private static final char TOGGLE_MAIN_VIEW = 'm';
    private static final char SUSPEND = 'z';
    private static final char ZOOM = 'z';
    private static final char NEXT_WAITING = 'w';
    private static final char YES = 'y';
    private static final char NO = 'n';
    private static final char OPEN_REVIEW = 'p';
    private static final char REMOVE_WORKTREE = 'd';
    private static final char LEFT = 'h';
    private static final char DOWN = 'j';
    private static final char UP = 'k';
    private static final char RIGHT = 'l';
    private static final char TOP = 'g';
    private static final char BOTTOM = 'G';

    private static final String MOVE_KEYS = "j/k move";
    private static final String SCROLL_KEYS = "j/k scroll  ^d/^u half page  g/G top/bottom";
    private static final String PANEL_KEYS = "h/l panel";
    private static final String FOLD_KEY = "enter fold";
    private static final String SESSIONS_KEY = "enter sessions";
    private static final String EXPAND_KEY = "enter expand";
    private static final String RESUME_KEY = "enter resume";
    private static final String FOCUS_KEYS = "%s focus";
    private static final String QUIT_KEY = "q quit";
    private static final String REFRESH_KEY = "R refresh";
    private static final String REVIEW_KEY = "p review";
    private static final List<String> RESPOND_KEYS = List.of("y start  esc back");
    private static final String REMOVE_WORKTREE_KEY = "d remove worktree";
    private static final String NEW_KEY = "N new";
    private static final String NEW_TICKET_KEY = "n new";
    private static final String MAIN_VIEW_KEY = "m main view";
    private static final String SESSION_KEYS = "%s tab  %s focus  %s main view  %s zoom";
    private static final String ZOOM_KEY = "z zoom";
    private static final String STATUS_COLUMNS_ZOOM_KEYS = "h/l column  j/k move  enter pick  z/esc back";
    private static final String REVIEW_ZOOM_KEY = "z back";
    private static final String MAIN_VIEW_ZOOM_KEYS = "j/k scroll  ^d/^u half page  g/G top/bottom  z/esc back";
    private static final String DOCK_ZOOM_KEYS = "%s tab  %s focus  %s main view  %s back";

    private final BoardRows board;
    private final DocsRows docs;
    private final SessionRows sessions;
    private final AtomicReference<DocsView> pendingDocs = new AtomicReference<>();
    private final AtomicReference<SessionsView> pendingSessions = new AtomicReference<>();
    private final AtomicReference<PullRequestsView> pendingPullRequests = new AtomicReference<>();
    private final AtomicReference<BoardView> pendingBoard = new AtomicReference<>();
    private final AtomicReference<WarningView> pendingWarning = new AtomicReference<>();
    private final MainViewScroll mainViewScroll = new MainViewScroll();
    private final HostedSessionCommand hosting;
    private final WatchedSessionCommand watching;
    private final TicketWorktreeCommand removing;
    private final TerminalFocus terminalFocus = new TerminalFocus(this::terminalFocusChanged);
    private WatchedPayload lastWatched = new NoWatchedSessionPayload();
    private CompletableFuture<Void> watchUpdates = CompletableFuture.completedFuture(null);
    private final SessionQuery screens;
    private final BoardQuery boards;
    private final DocsQuery docsQuery;
    private final ReviewQuery reviews;
    private final ReviewKeys reviewKeys;
    private final Path root;
    private final DockTabs dock = new DockTabs();
    private Map<SessionId, PaneSize> paneSizes = Map.of();
    private final AtomicBoolean repaintPending = new AtomicBoolean();
    private PullRequestsView pullRequests;
    private volatile BoardView lastRead;
    private volatile Set<String> mergedTicketBranches = Set.of();
    private volatile Set<String> latestMergedHeads = Set.of();
    private volatile List<LinkedTicketId> linkedTicketIds;
    private volatile Set<String> mergedHeads = Set.of();
    private final AtomicReference<Set<String>> pendingMergedHeads = new AtomicReference<>();
    private final Executor background;
    private final ReservedChords chords;
    private final ChordKeys chordKeys;
    private Optional<ReviewPane> review = Optional.empty();
    private Optional<VerbPicker> picker = Optional.empty();
    private Optional<Rect> sessionsArea = Optional.empty();
    private boolean mergedRead;
    private Optional<Rect> paneArea = Optional.empty();
    private Optional<Long> quitPrompt = Optional.empty();
    private Optional<RemovalPrompt> removalPrompt = Optional.empty();
    private Optional<TicketView> respondPrompt = Optional.empty();
    private Optional<String> refusal = Optional.empty();
    private final WorktreeDetail worktree;
    private final PendingDetail pendingDetail;
    private volatile MergedPullRequestsView latestMerged = new MergedPullRequestsView(List.of());
    private Optional<Rect> mainViewArea = Optional.empty();
    private CompletableFuture<Void> ordered = CompletableFuture.completedFuture(null);
    private boolean mainViewHidden;
    private Focus focus = Focus.BOARD;
    private Focus lastLeft = Focus.BOARD;
    private Focus lastRight = Focus.MAIN;
    private Focus mainViewOwner = Focus.BOARD;
    /** Whether the focused pane fills the screen, its shape given by the focus: {@code focusOn} ends it whenever focus moves. */
    private boolean zoomed;
    private Optional<WarningView> warning = Optional.empty();
    private boolean quitRequested;

    public Shell(
            BoardView board,
            DocsView docs,
            SessionsView sessions,
            PullRequestsView pullRequests,
            HostedSessionCommand hosting,
            WatchedSessionCommand watching,
            TicketWorktreeCommand removing,
            SessionQuery screens,
            BoardQuery boards,
            DocsQuery docsQuery,
            ReviewQuery reviews,
            PendingReviewCommand writing,
            ReservedChords chords) {
        this(
                board,
                docs,
                sessions,
                pullRequests,
                hosting,
                watching,
                removing,
                screens,
                boards,
                docsQuery,
                reviews,
                writing,
                chords,
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
            TicketWorktreeCommand removing,
            SessionQuery screens,
            BoardQuery boards,
            DocsQuery docsQuery,
            ReviewQuery reviews,
            PendingReviewCommand writing,
            ReservedChords chords,
            Executor background) {
        this.background = background;
        this.chords = chords;
        this.chordKeys = new ChordKeys(chords);
        this.board = new BoardRows(board);
        this.lastRead = board;
        this.linkedTicketIds = BoardRows.ticketIdsOf(board);
        this.docs = new DocsRows(docs);
        this.sessions = new SessionRows(sessions);
        this.pullRequests = pullRequests;
        this.hosting = hosting;
        this.watching = watching;
        this.removing = removing;
        this.screens = screens;
        this.boards = boards;
        this.docsQuery = docsQuery;
        this.reviews = reviews;
        this.root = board.root().value();
        this.reviewKeys = new ReviewKeys(writing, root, background, new ReviewHost(
                this::focus,
                this::backFromReview,
                reason -> refusal = Optional.of(reason),
                this::submitted,
                this::onRenderThread,
                pane -> review.filter(open -> open == pane).isPresent()));
        this.worktree = new WorktreeDetail(screens, background, this::onRenderThread);
        this.pendingDetail = new PendingDetail(reviews, root, background, this::onRenderThread);
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
        worktree.markStale();
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
        latestMerged = view;
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
        worktree.markStale();
        picker.ifPresent(open -> {
            open.checked();
            open.preselecting(choiceFor(open.ticket()));
        });
        refreshOffer();
    }

    private void applyPendingBoard() {
        BoardView next = pendingBoard.getAndSet(null);
        if (next != null) {
            worktree.markStale();
            RowKey before = board.selected().key();
            if (inStatusColumns()) {
                board.replaceInStatusColumns(next);
            } else {
                board.replace(next);
            }
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
     * terminal both have focus. It runs a process when it closes a notification, so off the render thread, and each change waits
     * for the one before it: otherwise an earlier one could land last and leave a session watched that no longer is.
     */
    private void syncWatched() {
        WatchedPayload now = inSession() && terminalFocus.focused()
                ? dock.selectedTab()
                        .flatMap(HostedSessionView::live)
                        .<WatchedPayload>map(live -> new WatchedSessionPayload(live.id()))
                        .orElseGet(NoWatchedSessionPayload::new)
                : new NoWatchedSessionPayload();
        if (!now.equals(lastWatched)) {
            lastWatched = now;
            watchUpdates = watchUpdates.exceptionally(failed -> null).thenRunAsync(() -> watching.watching(now), background);
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
        refusal = Optional.empty();
        return answerPrompt(key).orElseGet(() -> pressWithoutPrompt(key));
    }

    private Optional<Boolean> answerPrompt(KeyEvent key) {
        return quitPrompt
                .map(_ -> answerQuitPrompt(key))
                .or(() -> removalPrompt.map(asked -> answerRemovalPrompt(asked, key)))
                .or(() -> reviewKeys.answerSubmitPrompt(key))
                .or(() -> respondPrompt.map(asked -> answerRespondPrompt(asked, key)))
                .or(() -> picker.map(open -> pressInPicker(open, key)));
    }

    private boolean pressWithoutPrompt(KeyEvent key) {
        if (inSession()) {
            return pressInSession(key);
        }
        if (review.map(pane -> reviewKeys.press(pane, key)).orElse(false)) {
            return true;
        }
        return reservedBy(key)
                .map(this::pressReserved)
                .or(() -> pressZoom(key))
                .orElseGet(() -> pressUnzoomed(key));
    }

    private Optional<ReservedAction> reservedBy(KeyEvent key) {
        return TerminalInputs.key(key).flatMap(chords::actionOf);
    }

    /** What a reserved chord does outside a hosted session; rebinding {@code nextWaiting} keeps it leaving the zoom. */
    private boolean pressReserved(ReservedAction action) {
        return switch (action) {
            case FOCUS_LEFT -> focusOn(lastLeft);
            case FOCUS_RIGHT -> focusOn(lastRight);
            case FOCUS_DOWN -> focusOn(focus.below());
            case FOCUS_UP -> goUp(focus.above());
            case HIDE_MAIN_VIEW -> toggleMainView();
            case ZOOM -> toggleZoom();
            case NEXT_WAITING -> leaveZoom();
        };
    }

    /** The keys of the zoom: {@code z}, {@code esc} to leave, the cursor of the status columns, and {@code w}, which leaves. */
    private Optional<Boolean> pressZoom(KeyEvent key) {
        boolean plain = !key.hasCtrl() && !key.hasAlt();
        if (!plain) {
            return Optional.empty();
        }
        if (key.isChar(ZOOM) && (zoomed || focus == Focus.BOARD || focus == Focus.MAIN)) {
            return Optional.of(toggleZoom());
        }
        if (zoomed && (key.isKey(KeyCode.ESCAPE) || key.isChar(NEXT_WAITING))) {
            return Optional.of(leaveZoom());
        }
        return inStatusColumns() ? pressInStatusColumns(key) : Optional.empty();
    }

    private Optional<Boolean> pressInStatusColumns(KeyEvent key) {
        if (key.isKey(KeyCode.ENTER)) {
            return Optional.of(selectedTicket().isPresent() && leaveZoom() ? fold() : leaveZoom());
        }
        return statusColumnsMove(key).map(move -> select(() -> board.moveInStatusColumns(move)));
    }

    private static Optional<BiFunction<StatusColumns, TicketId, Optional<TicketView>>> statusColumnsMove(KeyEvent key) {
        if (key.isChar(DOWN)) {
            return Optional.of(StatusColumns::down);
        }
        if (key.isChar(UP)) {
            return Optional.of(StatusColumns::up);
        }
        if (key.isChar(LEFT)) {
            return Optional.of(StatusColumns::left);
        }
        return key.isChar(RIGHT) ? Optional.of(StatusColumns::right) : Optional.empty();
    }

    /** Zooms whatever has focus that has a zoom to show, or leaves the zoom. */
    private boolean toggleZoom() {
        if (zoomed) {
            return leaveZoom();
        }
        zoomed = focus == Focus.BOARD || focus == Focus.MAIN || focus == Focus.DOCK;
        if (inStatusColumns()) {
            select(board::enterStatusColumns);
        }
        return zoomed;
    }

    /** Ends the zoom, and says whether there was one. */
    private boolean leaveZoom() {
        boolean wasZoomed = zoomed;
        zoomed = false;
        return wasZoomed;
    }

    /** {@code esc} in a review: out of the zoom first, out of the review once there is none. */
    private boolean backFromReview() {
        return leaveZoom() || leaveReview();
    }

    private boolean inStatusColumns() {
        return zoomed && focus == Focus.BOARD;
    }

    private boolean pressUnzoomed(KeyEvent key) {
        if (key.hasCtrl()) {
            return TerminalInputs.isFixed(key, FixedChord.QUIT) ? requestQuit() : focus == Focus.MAIN && pressWithCtrl(key);
        }
        if (key.hasAlt()) {
            return false;
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
        if (key.isChar(REMOVE_WORKTREE)) {
            return removableTicket().map(this::checkRemoval).orElse(false);
        }
        if (key.isChar(OPEN_REVIEW)) {
            return openReview();
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
        return focus == Focus.MAIN && scrollMainView(key);
    }

    /** Whether a hosted session's tab has focus, so that every key but the reserved chords goes to its {@code claude}. */
    private boolean inSession() {
        return focus == Focus.DOCK && dock.selectedTab().isPresent();
    }

    private boolean pressInSession(KeyEvent key) {
        if (reservedBy(key).map(this::pressReservedInSession).orElse(false)) {
            return true;
        }
        if (key.hasCtrl() && key.isChar(SUSPEND)) {
            return true;
        }
        TerminalInputs.key(key).ifPresent(this::sendToSelectedTab);
        return true;
    }

    private boolean pressReservedInSession(ReservedAction action) {
        return switch (action) {
            case FOCUS_LEFT -> dock.selectPrevious() || focusOn(lastLeft);
            case FOCUS_RIGHT -> {
                dock.selectNext();
                yield true;
            }
            case FOCUS_UP -> goUp(Focus.MAIN);
            case HIDE_MAIN_VIEW -> toggleMainView();
            case ZOOM -> toggleZoom();
            case NEXT_WAITING -> {
                leaveZoom();
                yield true;
            }
            case FOCUS_DOWN -> true;
        };
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

    /** A mouse event over the pane reaches the selected tab's session at the matching cell, whatever has focus, while the pane is drawn. */
    boolean mouse(MouseEvent event) {
        return dock.selectedTab()
                .filter(_ -> !zoomed || focus == Focus.DOCK)
                .flatMap(tab -> paneArea.flatMap(area -> TerminalInputs.mouse(event, area))
                        .map(input -> {
                            hosting.send(new HostedInputPayload(tab.id(), input));
                            return true;
                        }))
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

    /** Commands that touch processes or files run off the render thread, each after the one before it. */
    private void inOrder(Runnable command) {
        ordered = ordered.exceptionally(failed -> null).thenRunAsync(command, background);
    }

    /**
     * A hosted session running {@code /ticket done} on {@code ticket} has ended. Callable from any thread: the board is read again, off
     * the render thread, and when the ticket now reads done the same checks as {@code d} run and the question is asked.
     */
    public void ticketDoneEnded(LinkedTicketId ticket) {
        inOrder(() -> {
            reread();
            BoardRows.ticketOf(lastRead, ticket)
                    .filter(found -> found.status().finished())
                    .ifPresent(found -> askRemoval(found, false));
        });
    }

    /** The selected ticket when {@code d} applies: on the Board, reading done, and with a worktree last read. */
    private Optional<TicketView> removableTicket() {
        return selectedTicket()
                .filter(ticket -> focus == Focus.BOARD && ticket.status().finished())
                .filter(ticket -> worktree.of(ticket).filter(HeldWorktreeView.class::isInstance).isPresent());
    }

    private boolean checkRemoval(TicketView ticket) {
        inOrder(() -> askRemoval(ticket, true));
        return true;
    }

    /**
     * Judges the ticket's worktree afresh, off the render thread, and on the render thread asks whether to remove it when nothing
     * stands in the way, else says what does. With {@code stillSelected}, an answer about a ticket no longer selected is dropped; without
     * it, the check was raised by a session's end rather than asked for, and a ticket with no worktree has nothing to refuse.
     */
    private void askRemoval(TicketView ticket, boolean stillSelected) {
        WorktreeView judged = screens.worktreeOf(worktreePayload(ticket));
        onRenderThread(() -> {
            if (stillSelected && !selectedTicket().map(TicketView::id).equals(Optional.of(ticket.id()))) {
                return;
            }
            if (!stillSelected && judged instanceof NoWorktreeView) {
                return;
            }
            if (judged instanceof HeldWorktreeView held && held.clear()) {
                removalPrompt = Optional.of(new RemovalPrompt(ticket, held.path()));
            } else {
                refusal = Optional.of(WorktreeText.refusal(judged));
            }
        });
    }

    private WorktreePayload worktreePayload(TicketView ticket) {
        return new WorktreePayload(
                new SessionPayload(root, BoardRows.ticketIdsOf(lastRead)),
                ticket.branch().value(),
                latestMerged.mergedHeadOf(new HeadBranch(ticket.branch().value())));
    }

    private boolean answerRemovalPrompt(RemovalPrompt asked, KeyEvent key) {
        removalPrompt = Optional.empty();
        if (key.isChar(YES) && !key.hasCtrl() && !key.hasAlt()) {
            inOrder(() -> {
                WorktreeRemovalView result = removing.remove(worktreePayload(asked.ticket()));
                onRenderThread(() -> removed(result));
            });
        }
        return true;
    }

    private void removed(WorktreeRemovalView result) {
        switch (result) {
            case RemovedWorktreeView _ -> worktree.forget();
            case RefusedRemovalView refused -> refusal = Optional.of(WorktreeText.refusal(refused.state()));
            case FailedRemovalView failed -> refusal = Optional.of(WorktreeText.failure(failed.reason()));
        }
    }

    /** Reads everything again, off the render thread, and shows only what changed; the pull requests are fetched on their own thread. */
    private boolean refresh() {
        pendingDetail.markStale();
        review.ifPresent(this::readReview);
        background.execute(() -> {
            reviews.refresh();
            screens.refresh(new SessionPayload(root, linkedTicketIds));
            docsChanged(docsQuery.docsFor(new DocsPayload(root)));
            boardChanged();
        });
        return true;
    }

    /** The open pull request whose head is the selected ticket's ticket branch, when {@code p} is to open its review. */
    private Optional<PullRequestView> reviewablePullRequest() {
        return (focus == Focus.BOARD || focus == Focus.SESSIONS) && review.isEmpty()
                ? selectedTicket().flatMap(ticket -> PullRequestText.pullRequestOf(ticket, pullRequests))
                : Optional.empty();
    }

    private boolean openReview() {
        return reviewablePullRequest()
                .map(pullRequest -> {
                    ReviewPane pane = new ReviewPane(
                            pullRequest.number(),
                            pullRequest.title(),
                            reviews.loadingDiff(),
                            reviews.loadingThreads(),
                            reviews.loadingPending(),
                            reviews);
                    review = Optional.of(pane);
                    lastLeft = Focus.FILES;
                    goUp(Focus.MAIN);
                    readReview(pane);
                    return true;
                })
                .orElse(false);
    }

    private boolean leaveReview() {
        review = Optional.empty();
        pendingDetail.markStale();
        lastLeft = Focus.BOARD;
        return focusOn(Focus.BOARD);
    }

    /**
     * Reads the review's diff and threads off the render thread, each landing on the render thread. A read lands only in the pane it was
     * begun for and only if no later read of its kind was begun, so one that is slow never shows over a newer answer or a later review.
     */
    private void readReview(ReviewPane pane) {
        PullRequestPayload payload = new PullRequestPayload(root, pane.number());
        long diffRead = pane.beginDiffRead();
        long threadsRead = pane.beginThreadsRead();
        long pendingRead = pane.beginPendingRead();
        CompletableFuture.supplyAsync(() -> reviews.pendingOf(payload), background)
                .exceptionally(ReviewReads::unreadablePending)
                .thenAccept(read -> onRenderThread(() -> landed(pane, () -> pane.applyPending(pendingRead, read))));
        CompletableFuture.supplyAsync(() -> reviews.diffOf(payload), background)
                .exceptionally(ReviewReads::unreadableDiff)
                .thenAccept(read -> onRenderThread(() -> landed(pane, () -> pane.applyDiff(diffRead, read))));
        CompletableFuture.supplyAsync(() -> reviews.threadsOf(payload), background)
                .exceptionally(ReviewReads::unreadableThreads)
                .thenAccept(read -> onRenderThread(() -> landed(pane, () -> pane.applyThreads(threadsRead, read))));
    }

    private void landed(ReviewPane pane, Runnable apply) {
        review.filter(open -> open == pane).ifPresent(_ -> apply.run());
    }

    /** GitHub took the submit: the review closes and the ticket is selected, after offering to respond when the ticket's status allows it. */
    private void submitted() {
        Optional<TicketView> ticket = selectedTicket().filter(found -> found.verbs().contains(Verb.RESPOND));
        if (ticket.isPresent()) {
            respondPrompt = ticket;
        } else {
            leaveReview();
        }
    }

    private boolean answerRespondPrompt(TicketView ticket, KeyEvent key) {
        if (key.isChar(YES) && !key.hasCtrl() && !key.hasAlt()) {
            respondPrompt = Optional.empty();
            leaveReview();
            TicketHostedPayload payload = new TicketHostedPayload(
                    root, new LinkedTicketId(ticket.id().value()), ticket.branch().value(), Verb.RESPOND, paneSize());
            inBackground(() -> hosting.startTicket(payload));
        } else if (key.isKey(KeyCode.ESCAPE)) {
            respondPrompt = Optional.empty();
            leaveReview();
        }
        return true;
    }

    private boolean toggleMainView() {
        leaveZoom();
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
            case NotStartedHost _ -> {}
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
        List<SessionId> resized = dock.tabs().stream()
                .map(HostedSessionView::id)
                .filter(id -> !size.equals(paneSizes.get(id)))
                .toList();
        resized.forEach(id -> hosting.resize(new ResizePayload(id, size)));
        sized(resized, size);
    }

    /** Remembers that the ptys of {@code ids} now have {@code size}. */
    private void sized(List<SessionId> ids, PaneSize size) {
        paneSizes = Stream.concat(
                        paneSizes.entrySet().stream().filter(sized -> !ids.contains(sized.getKey())),
                        ids.stream().map(id -> Map.entry(id, size)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean pressWithCtrl(KeyEvent key) {
        if (TerminalInputs.isFixed(key, FixedChord.HALF_PAGE_DOWN)) {
            mainViewScroll.halfPageDown();
            return true;
        }
        if (TerminalInputs.isFixed(key, FixedChord.HALF_PAGE_UP)) {
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
        Optional<LinkedTicketId> ticket = board.selected() instanceof TicketRow(TicketView selected)
                ? Optional.of(new LinkedTicketId(selected.id().value()))
                : Optional.empty();
        sessions.showTicket(ticket);
        refreshOffer();
    }

    private Optional<TicketView> selectedTicket() {
        return board.selected() instanceof TicketRow(TicketView ticket) ? Optional.of(ticket) : Optional.empty();
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
        leaveZoom();
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

    private boolean selectSession(Runnable move) {
        Optional<RowKey> before = sessions.selectedKey();
        move.run();
        if (!sessions.selectedKey().equals(before)) {
            mainViewScroll.reset();
        }
        return true;
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
            case LiveSessionRow live -> focusTabOf(live.session());
            case PastSessionRow past when past.session().isUnlinked() -> resume(past.session());
            case PastSessionRow past -> selectedTicket().map(ticket -> resumeInTicket(past.session(), ticket)).orElse(false);
            case StartingSessionRow starting -> focusTab(starting.tab().id());
            case NewSessionRow _ -> selectedTicket().map(this::newSessionFor).orElse(false);
            default -> false;
        };
    }

    private boolean focusTab(SessionId id) {
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
        if (target != focus) {
            zoomed = false;
        }
        focus = target;
        if (target.column() == Column.LEFT) {
            lastLeft = target;
        } else {
            lastRight = target;
        }
        if (target.column() == Column.LEFT && !target.reviewing() && target != mainViewOwner) {
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
        syncWorktree();
        syncPending();
        Element screen = column(
                new StatusLineElement(
                        StatusText.spans(sessions.markerCounts()),
                        warning,
                        quitPrompt
                                .map(SessionText::quitPrompt)
                                .or(() -> removalPrompt.map(asked -> WorktreeText.prompt(root, asked.worktree())))
                                .or(reviewKeys::prompt)),
                panes(),
                new KeyBarElement(keyBar()));
        Element withPicker = picker.<Element>map(open -> stack(
                        screen,
                        new PopupElement(
                                VerbPickerText.title(open),
                                VerbPickerText.rows(open),
                                Optional.of(open.cursor()),
                                VerbPickerText.hint(),
                                () -> sessionsArea)))
                .orElse(screen);
        Element withRespond = respondPrompt
                .<Element>map(ticket -> stack(
                        withPicker,
                        new PopupElement(
                                RESPOND_TITLE,
                                List.of(RESPOND_PROMPT.formatted(ticket.id().value())),
                                Optional.empty(),
                                RESPOND_HINT,
                                () -> mainViewArea)))
                .orElse(withPicker);
        Element withBox = review.flatMap(ReviewPane::box)
                .filter(open -> open.inlineAt().isEmpty())
                .<Element>map(open -> stack(withRespond, ReviewPanels.boxPopup(open, () -> mainViewArea)))
                .orElse(withRespond);
        return withBox;
    }

    /** The panels, or the focused pane zoomed: the Board as its status columns, the main view, or the dock. */
    private Element panes() {
        if (!zoomed) {
            return row(leftColumn(), (mainViewHidden ? column(dock()) : column(mainView(), dock())).fill()).fill();
        }
        return switch (focus) {
            case BOARD ->
                column(StatusColumnsPanels.of(board.statusColumns(), selectedTicket().map(TicketView::id), this::statusColumnsLine), dockStrip())
                        .fill();
            case MAIN -> column(mainView(), dockStrip()).fill();
            default -> dock();
        };
    }

    private Element dockStrip() {
        return new DockStripElement(DockText.strip(dock.tabs(), dock.selectedIndex()));
    }

    private RowLine statusColumnsLine(TicketView ticket) {
        return boardLine(new TicketRow(ticket));
    }

    private Element leftColumn() {
        return review.<Element>map(pane -> column(ReviewPanels.files(pane, focus), ReviewPanels.comments(pane, focus), ReviewPanels.review(pane))
                        .percent(LEFT_COLUMN_PERCENT))
                .orElseGet(() -> column(boardPanel(), sessionsPanel(), docsPanel()).percent(LEFT_COLUMN_PERCENT));
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
        Element body = sessions.isEmpty()
                ? text(NO_LIVE_SESSION).dim()
                : new SessionRowsElement(sessions.visible(), sessions.selectedIndex(), focus == Focus.SESSIONS);
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

    private Element mainView() {
        return review.map(pane -> ReviewPanels.diff(pane, focus, area -> mainViewArea = Optional.of(area)))
                .orElseGet(() -> framed(MAIN_VIEW_TITLE, Focus.MAIN, new PaneProbe(detail(), area -> mainViewArea = Optional.of(area))).fill());
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
        return Frames.framed(title, body, focus == owner);
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
                        new DetailElement(width -> SessionText.card(past.session()), Optional.empty(), mainViewScroll);
                    case OlderSessionsRow _ -> column();
                    case SectionHeaderRow _ -> column();
                    case NoSessionRow _ -> column();
                    case NewSessionRow _ -> column();
                    case StartingSessionRow _ -> column();
                })
                .orElseGet(Toolkit::column);
    }

    private void onRenderThread(Runnable task) {
        ToolkitRunner running = runner();
        if (running == null) {
            task.run();
        } else {
            running.runOnRenderThread(task);
        }
    }

    private Element docsDetail() {
        return docs.selected()
                .map(row -> switch (row) {
                    case FolderRow _ -> column();
                    case DocumentRow document -> documentDetail(document.document());
                })
                .orElseGet(Toolkit::column);
    }

    private Element documentDetail(DocumentView document) {
        return new DetailElement(width -> DocsText.head(document), DocsText.markdown(document), mainViewScroll);
    }

    private Element boardDetail() {
        return switch (board.selected()) {
            case HeaderRow _ -> column();
            case TicketRow row -> ticketDetail(row.ticket());
            case UnreadableRow row ->
                column(text(row.unreadable().file().value()).bold(), text(row.unreadable().reason().value()));
        };
    }

    /** Reads the selected ticket's worktree while the Board owns the main view. */
    private void syncWorktree() {
        Optional<TicketView> wanted = mainViewOwner == Focus.BOARD ? selectedTicket() : Optional.empty();
        wanted.ifPresent(ticket -> worktree.sync(ticket, () -> worktreePayload(ticket)));
    }

    /** Reads the pending review of the selected ticket's open pull request while the Board owns the main view and no review is open. */
    private void syncPending() {
        if (review.isEmpty() && mainViewOwner == Focus.BOARD) {
            selectedTicket().ifPresent(ticket -> PullRequestText.pullRequestOf(ticket, pullRequests)
                    .ifPresent(pullRequest -> pendingDetail.sync(ticket, pullRequest.number())));
        }
    }

    private Optional<WorktreeFacts> worktreeDetail(TicketView ticket) {
        return worktree.of(ticket).flatMap(view -> WorktreeText.detail(root, view));
    }

    private Element ticketDetail(TicketView ticket) {
        return new DetailElement(width -> TicketText.head(ticket, pullRequests, pendingDetail.of(ticket), worktreeDetail(ticket), width), TicketText.markdown(ticket), mainViewScroll);
    }

    private List<String> keyBar() {
        return refusal
                .map(List::of)
                .or(() -> picker.map(_ -> List.of(VerbPickerText.keys())))
                .or(() -> Optional.of(zoomKeys()).filter(_ -> inSession() && zoomed))
                .or(() -> Optional.of(sessionKeys(SESSION_KEYS)).filter(_ -> inSession()).map(List::of))
                .or(() -> respondPrompt.map(_ -> RESPOND_KEYS))
                .or(() -> review.map(this::reviewKeyBar))
                .or(() -> Optional.of(zoomKeys()).filter(_ -> zoomed))
                .orElseGet(this::boardKeys);
    }

    /** A zoomed diff puts its way back first, where the clipped key bar still shows it; the panel and focus keys would leave the zoom. */
    private List<String> reviewKeyBar(ReviewPane pane) {
        return zoomed
                ? reviewKeys.keys(pane, focus, List.of(REVIEW_ZOOM_KEY), List.of())
                : reviewKeys.keys(pane, focus, List.of(), List.of(PANEL_KEYS, focusKeys(), QUIT_KEY));
    }

    private String focusKeys() {
        return FOCUS_KEYS.formatted(chordKeys.focus());
    }

    private String sessionKeys(String format) {
        return format.formatted(chordKeys.tab(), chordKeys.focus(), chordKeys.hideMainView(), chordKeys.zoom());
    }

    private List<String> zoomKeys() {
        return List.of(
                switch (focus) {
                    case BOARD -> STATUS_COLUMNS_ZOOM_KEYS;
                    case MAIN -> MAIN_VIEW_ZOOM_KEYS;
                    default -> sessionKeys(DOCK_ZOOM_KEYS);
                });
    }

    private List<String> boardKeys() {
        return Stream.of(
                        Included.when(focus.column() == Column.LEFT, MOVE_KEYS),
                        Included.when(focus == Focus.MAIN, SCROLL_KEYS),
                        Stream.of(PANEL_KEYS),
                        Included.when(foldable(), FOLD_KEY),
                        Included.when(focus == Focus.BOARD && board.selected() instanceof TicketRow, SESSIONS_KEY),
                        Included.when(focus == Focus.SESSIONS && sessions.olderSelected(), EXPAND_KEY),
                        Included.when(focus == Focus.SESSIONS && sessions.pastSelected(), RESUME_KEY),
                        Included.when(removableTicket().isPresent(), REMOVE_WORKTREE_KEY),
                        Stream.of(focusKeys(), QUIT_KEY),
                        Included.when(
                                (focus == Focus.BOARD || focus == Focus.SESSIONS) && selectedTicket().isPresent(),
                                NEW_TICKET_KEY),
                        Included.when(reviewablePullRequest().isPresent(), REVIEW_KEY),
                        Stream.of(NEW_KEY, REFRESH_KEY, MAIN_VIEW_KEY),
                        Included.when(focus == Focus.BOARD || focus == Focus.MAIN, ZOOM_KEY))
                .flatMap(Function.identity())
                .toList();
    }

    private boolean foldable() {
        return focus == Focus.BOARD && board.selectedHeader().isPresent()
                || focus == Focus.DOCS && docs.selectedFolder().isPresent();
    }
}
