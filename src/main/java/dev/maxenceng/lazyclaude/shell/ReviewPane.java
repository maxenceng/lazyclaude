package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsInDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/**
 * What the review screen shows of one pull request: its diff and threads as last read, the cursor over the diff lines, and the Files
 * and Comments selections. A read lands only if no later read of the same kind was begun since, so one that was slow never replaces a
 * newer answer; the screen throws the whole pane away when the review is left, which drops every read still under way.
 */
final class ReviewPane {

    private static final int HALF = 2;

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
        this.placed = reviews.threadsInDiff(diff, threads);
        this.placedPending = reviews.pendingInDiff(diff, pending);
        this.rows = DiffRows.of(diff, placed, placedPending, showResolved);
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
        if (rows.lineCount() > 0) {
            rows.firstLineOfNextFile(cursor).ifPresent(this::moveTo);
        }
    }

    void previousFile() {
        if (rows.lineCount() > 0) {
            rows.firstLineOfPreviousFile(cursor).ifPresent(this::moveTo);
        }
    }

    void selectFileDown() {
        selectedFile = clamp(selectedFile + 1, rows.files().size());
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

    void selectCommentUp() {
        selectedComment = clamp(selectedComment - 1, entries().size());
    }

    /** To the line the selected thread shows under; nowhere for an outdated one. */
    void goToSelectedComment() {
        if (selectedComment < entries().size()) {
            if (lineOf(entries().get(selectedComment)) instanceof OnDiffLine on) {
                moveTo(on.index().value());
            }
        }
    }

    private static ThreadLine lineOf(CommentEntry entry) {
        return switch (entry) {
            case PendingEntry pending -> pending.placed().line();
            case ThreadEntry thread -> thread.placed().line();
        };
    }

    private void rebuild() {
        placed = reviews.threadsInDiff(diff, threads);
        placedPending = reviews.pendingInDiff(diff, pending);
        rows = DiffRows.of(diff, placed, placedPending, showResolved);
        moveTo(cursor);
    }

    /** The cursor on {@code line}, held within the lines there are; the Files selection goes to the cursor's file. */
    private void moveTo(int line) {
        cursor = clamp(line, rows.lineCount());
        selectedFile = rows.lineCount() == 0 ? clamp(selectedFile, rows.files().size()) : rows.fileOfLine(cursor);
    }

    private int halfPage() {
        return Math.max(1, viewHeight / HALF);
    }

    private static int clamp(int index, int size) {
        return Math.clamp(index, 0, Math.max(0, size - 1));
    }
}
