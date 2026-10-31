package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.CommentAnchorPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.CommentAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.OnDiffAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.OpenPendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.RefusedAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;
import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/**
 * What the review screen shows of one pull request: its diff and threads as last read, the cursor over the diff lines, and the Files
 * and Comments selections. A read lands only if no later read of the same kind was begun since, so one that was slow never replaces a
 * newer answer; the screen throws the whole pane away when the review is left, which drops every read still under way.
 */
final class ReviewPane {

    private static final Summary NO_SUMMARY = new Summary("");
    private static final int HALF = 2;
    private static final int WHEEL_LINES = 3;

    private final PullRequestNumber number;
    private final PullRequestTitle title;
    private final ReviewQuery reviews;
    private PullRequestDiffView diff;
    private ThreadsView threads;
    private ThreadsInDiffView placed;
    private PendingReviewView pending;
    private PendingCommentsInDiffView placedPending;
    private boolean showResolved;
    private DiffRows rows;
    private int cursor;
    private int selectedFile;
    private int selectedComment;
    private int top;
    private int viewHeight;
    private long diffReads;
    private long threadsReads;
    private long pendingReads;
    private Optional<CommentBox> box = Optional.empty();
    // deferred: an unsubmitted summary does not survive leaving the review — GitHub accepting an edit of an empty-bodied pending review.
    private Optional<Summary> typedSummary = Optional.empty();
    private Optional<Integer> rangeStart = Optional.empty();
    private Optional<Integer> rangeEnd = Optional.empty();

    ReviewPane(
            PullRequestNumber number,
            PullRequestTitle title,
            PullRequestDiffView diff,
            ThreadsView threads,
            PendingReviewView pending,
            ReviewQuery reviews) {
        this.number = number;
        this.title = title;
        this.diff = diff;
        this.threads = threads;
        this.pending = pending;
        this.reviews = reviews;
        this.placed = reviews.threadsInDiff(new ThreadsInDiffPayload(diff, threads));
        this.placedPending = reviews.pendingInDiff(new PendingCommentsInDiffPayload(diff, pending));
        this.rows = DiffRows.of(diff, placed, placedPending, inlineBox(), showResolved);
    }

    PullRequestNumber number() {
        return number;
    }

    PullRequestTitle title() {
        return title;
    }

    PullRequestDiffView diff() {
        return diff;
    }

    ThreadsView threads() {
        return threads;
    }

    PendingReviewView pending() {
        return pending;
    }

    /** The pending comments in diff order, the ones on no line last; none until the pending review is read. */
    List<PendingInDiffView> pendingComments() {
        return placedPending.comments();
    }

    /** How many pending comments hang on the file at {@code path}. */
    int pendingIn(DiffPath path) {
        return (int) pendingComments().stream().filter(placed -> placed.comment().anchor().path().equals(path)).count();
    }

    DiffRows rows() {
        return rows;
    }

    boolean showResolved() {
        return showResolved;
    }

    /** Which diff line the cursor is on; 0 when the diff holds none. */
    int cursor() {
        return cursor;
    }

    int selectedFile() {
        return selectedFile;
    }

    int selectedComment() {
        return selectedComment;
    }

    /** The first row of the main view that is drawn. */
    int top() {
        return top;
    }

    void top(int row) {
        top = row;
    }

    /** Takes the height the main view was last drawn at, which is what half a page is of. */
    void observeHeight(int height) {
        viewHeight = height;
    }

    /** Starts a read of the diff; only the answer to the latest one started is taken. */
    long beginDiffRead() {
        diffReads++;
        return diffReads;
    }

    long beginThreadsRead() {
        threadsReads++;
        return threadsReads;
    }

    void applyDiff(long read, PullRequestDiffView view) {
        if (read == diffReads) {
            diff = view;
            rebuild();
        }
    }

    void applyThreads(long read, ThreadsView view) {
        if (read == threadsReads) {
            threads = view;
            rebuild();
            selectedComment = clamp(selectedComment, entries().size());
        }
    }

    long beginPendingRead() {
        pendingReads++;
        return pendingReads;
    }

    void applyPending(long read, PendingReviewView view) {
        if (read == pendingReads) {
            pending = view;
            rebuild();
            selectedComment = clamp(selectedComment, entries().size());
        }
    }

    void toggleResolved() {
        showResolved = !showResolved;
        rebuild();
    }

    /** The threads in diff order, the ones on no line last; none until they are read. */
    List<ThreadInDiffView> comments() {
        return placed.threads();
    }

    /** What Comments lists: the pending comments first, in diff order, then the threads in diff order. */
    List<CommentEntry> entries() {
        return Stream.concat(
                        pendingComments().stream().<CommentEntry>map(PendingEntry::new),
                        comments().stream().<CommentEntry>map(ThreadEntry::new))
                .toList();
    }

    /** How many threads of the file at {@code path} still wait for an answer: not resolved, and not outdated. */
    int openThreadsIn(DiffPath path) {
        return (int) comments().stream()
                .map(ThreadInDiffView::thread)
                .filter(thread -> thread.open() && thread.path().equals(path))
                .count();
    }

    /** The cursor on diff line {@code line}, held within the lines there are. */
    void cursorTo(int line) {
        moveTo(line);
    }

    /** One notch of the wheel over the diff: the cursor, and with it the view, three lines down or up. */
    void wheel(boolean down) {
        moveTo(down ? cursor + WHEEL_LINES : cursor - WHEEL_LINES);
    }

    void lineDown() {
        moveTo(cursor + 1);
    }

    void lineUp() {
        moveTo(cursor - 1);
    }

    void halfPageDown() {
        moveTo(cursor + halfPage());
    }

    void halfPageUp() {
        moveTo(cursor - halfPage());
    }

    /** To the first line of the next file that has lines; nowhere from the last. */
    void nextFile() {
        if (rows.hasLines()) {
            rows.firstLineOfNextFile(cursor).ifPresent(this::moveTo);
        }
    }

    void previousFile() {
        if (rows.hasLines()) {
            rows.firstLineOfPreviousFile(cursor).ifPresent(this::moveTo);
        }
    }

    void selectFileDown() {
        selectedFile = clamp(selectedFile + 1, rows.files().size());
    }

    /** Selects the file at {@code index}, held within the files there are. */
    void selectFile(int index) {
        selectedFile = clamp(index, rows.files().size());
    }

    void selectFileUp() {
        selectedFile = clamp(selectedFile - 1, rows.files().size());
    }

    void goToSelectedFile() {
        rows.firstLineOf(selectedFile).ifPresent(this::moveTo);
    }

    void selectCommentDown() {
        selectedComment = clamp(selectedComment + 1, entries().size());
    }

    /** Selects the comment at {@code index}, held within the entries there are. */
    void selectComment(int index) {
        selectedComment = clamp(index, entries().size());
    }

    void selectCommentUp() {
        selectedComment = clamp(selectedComment - 1, entries().size());
    }

    /** To the line the selected thread shows under; nowhere for an outdated one. */
    void goToSelectedComment() {
        if (selectedComment < entries().size()) {
            if (lineOf(entries().get(selectedComment)) instanceof OnDiffLine(DiffLineIndex index)) {
                moveTo(index.value());
            }
        }
    }

    private static ThreadLine lineOf(CommentEntry entry) {
        return switch (entry) {
            case PendingEntry pending -> pending.placed().line();
            case ThreadEntry thread -> thread.placed().line();
        };
    }

    void typeSummary(Summary summary) {
        typedSummary = Optional.of(summary);
    }

    /** What the summary box starts from and a submit sends: the one typed here, else the body of the adopted review, else nothing. */
    Summary summaryToSubmit() {
        return typedSummary.orElseGet(() -> pending instanceof OpenPendingReviewView open ? open.summary() : NO_SUMMARY);
    }

    Optional<CommentBox> box() {
        return box;
    }

    /** Opens the box; it is drawn under its line, or as a popup when it has none. */
    void openBox(CommentBox opened) {
        box = Optional.of(opened);
        rebuild();
    }

    /** Throws the box and what was typed in it away, and the range it was written on. */
    void closeBox() {
        box = Optional.empty();
        rangeStart = Optional.empty();
        rangeEnd = Optional.empty();
        rebuild();
    }

    /** Drops the range marked; answers whether there was one. */
    boolean clearRange() {
        boolean marked = rangeStart.isPresent();
        rangeStart = Optional.empty();
        rangeEnd = Optional.empty();
        return marked;
    }

    /** Whether the diff line at {@code line} is inside the range being marked. */
    boolean inRange(int line) {
        return rangeStart.isPresent() && line >= rangeLow() && line <= rangeHigh();
    }

    private int rangeLow() {
        return Math.min(rangeStart.orElse(cursor), rangeEnd.orElse(cursor));
    }

    private int rangeHigh() {
        return Math.max(rangeStart.orElse(cursor), rangeEnd.orElse(cursor));
    }

    /**
     * {@code v}: starts a range at the cursor, or ends the one started at it when its lines can take a comment. A range that cannot is
     * refused with the reason and left as it was.
     */
    Optional<Reason> markRange() {
        if (rangeStart.isEmpty() || rangeEnd.isPresent()) {
            rangeStart = Optional.of(cursor);
            rangeEnd = Optional.empty();
            return Optional.empty();
        }
        return rangeStart.flatMap(start -> markRange(start, cursor));
    }

    /**
     * A drag: the cursor goes to diff line {@code to}, and the range from {@code from} to it is marked when its lines can take a comment.
     * A range that cannot is refused with the reason, and the range marked before is left as it was.
     */
    Optional<Reason> markRange(int from, int to) {
        moveTo(to);
        int start = clamp(from, rows.lineCount());
        return switch (reviews.anchorOf(new CommentAnchorPayload(diff, new DiffLineIndex(Math.min(start, cursor)), new DiffLineIndex(Math.max(start, cursor))))) {
            case OnDiffAnchorView _ -> {
                rangeStart = Optional.of(start);
                rangeEnd = Optional.of(cursor);
                yield Optional.empty();
            }
            case RefusedAnchorView refused -> Optional.of(refused.reason());
        };
    }

    /** Where a comment would hang: the range marked, else the line the cursor is on. */
    CommentAnchorView anchor() {
        return reviews.anchorOf(new CommentAnchorPayload(diff, new DiffLineIndex(rangeStart.isPresent() ? rangeLow() : cursor), new DiffLineIndex(rangeStart.isPresent() ? rangeHigh() : cursor)));
    }

    /** The diff line a comment on the marked range or the cursor's line is drawn under: the last line of it. */
    int anchorLine() {
        return rangeStart.isPresent() ? rangeHigh() : cursor;
    }

    /** The pending comment on the cursor's line that was written last, if the line has one. */
    Optional<PendingCommentView> pendingOnCursorLine() {
        return pendingComments().stream()
                .filter(placed -> placed.line() instanceof OnDiffLine(DiffLineIndex index) && index.value() == cursor)
                .map(PendingInDiffView::comment)
                .reduce((_, later) -> later);
    }

    /** The row selected in Comments, if there is one. */
    Optional<CommentEntry> selectedEntry() {
        List<CommentEntry> entries = entries();
        return selectedComment < entries.size() ? Optional.of(entries.get(selectedComment)) : Optional.empty();
    }

    /** The diff line a thread or a pending comment shows under, if it shows under one. */
    Optional<Integer> lineOf(ThreadLine line) {
        return line instanceof OnDiffLine(DiffLineIndex index) ? Optional.of(index.value()) : Optional.empty();
    }

    /** The diff line the thread shows under, if it is placed. */
    Optional<Integer> lineOfThread(ReviewThreadView thread) {
        return comments().stream()
                .filter(placed -> placed.thread().id().equals(thread.id()))
                .findFirst()
                .flatMap(placed -> lineOf(placed.line()));
    }

    /** What a write answered with replaces what was read, and any read begun before it. */
    void applyWritten(PendingReviewView view) {
        pending = view;
        pendingReads++;
        rebuild();
        selectedComment = clamp(selectedComment, entries().size());
    }

    private Optional<CommentBox> inlineBox() {
        return box.filter(open -> open.inlineAt().isPresent());
    }

    private void rebuild() {
        placed = reviews.threadsInDiff(new ThreadsInDiffPayload(diff, threads));
        placedPending = reviews.pendingInDiff(new PendingCommentsInDiffPayload(diff, pending));
        rows = DiffRows.of(diff, placed, placedPending, inlineBox(), showResolved);
        moveTo(cursor);
    }

    /** The cursor on {@code line}, held within the lines there are; the Files selection goes to the cursor's file. */
    private void moveTo(int line) {
        cursor = clamp(line, rows.lineCount());
        selectedFile = !rows.hasLines() ? clamp(selectedFile, rows.files().size()) : rows.fileOfLine(cursor);
    }

    private int halfPage() {
        return Math.max(1, viewHeight / HALF);
    }

    private static int clamp(int index, int size) {
        return Math.clamp(index, 0, Math.max(0, size - 1));
    }
}
