package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.OnDiffAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingDeletePayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingEditPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReplyPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingSubmitPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.RefusedAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.RefusedWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.WrittenReviewView;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.session.published.FixedChord;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * The keys of an open review and the writes they make to your pending review: moving through the diff, Files and Comments, the comment
 * box, and the confirm after the summary. Writes run one after the other off the render thread, and an answer lands only in the review
 * it was written from.
 */
final class ReviewKeys {

    private static final char YES = 'y';
    private static final char OPEN_REVIEW = 'p';
    private static final char RESOLVED = 'r';
    private static final char COMMENT = 'c';
    private static final char RANGE = 'v';
    private static final char DELETE = 'x';
    private static final char SUMMARY = 'S';
    private static final char SAVE = 's';
    private static final char DOWN = 'j';
    private static final char UP = 'k';
    private static final char NEXT_FILE = 'J';
    private static final char PREVIOUS_FILE = 'K';

    private static final String NOT_DELETED = "not deleted (%s)";
    private static final String NOT_SUBMITTED = "not submitted (%s)";
    private static final String SUBMIT_PROMPT = "submit review as Comment? y/n";

    private static final List<String> DIFF_KEYS = List.of(
            "c comment", "v range", "enter edit", "x delete", "S submit", "j/k line", "J/K file", "^d/^u page", "r resolved", "R refresh", "esc back");
    private static final List<String> COMMENTS_KEYS = List.of(
            "c reply", "enter open", "x delete", "S submit", "j/k move", "r resolved", "R refresh", "esc back");
    private static final List<String> FILES_KEYS = List.of("j/k move", "enter go to line", "S submit", "r resolved", "R refresh", "esc back");
    private static final List<String> BOX_KEYS = List.of("^s save  esc cancel");
    private static final List<String> SUBMIT_KEYS = List.of("y submit  any other key keeps it pending");

    private final PendingReviewCommand writing;
    private final Path root;
    private final Executor background;
    private final ReviewHost host;
    private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
    private Optional<ReviewPane> submitPrompt = Optional.empty();

    /** {@code background} runs the writes; {@code host} is the shell the review is open in. */
    ReviewKeys(PendingReviewCommand writing, Path root, Executor background, ReviewHost host) {
        this.writing = writing;
        this.root = root;
        this.background = background;
        this.host = host;
    }

    /** The question the status line asks while the confirm waits. */
    Optional<String> prompt() {
        return submitPrompt.map(_ -> SUBMIT_PROMPT);
    }

    /** The keys the key bar offers in {@code pane}: the box's or the confirm's alone, else {@code before}, those of the focused panel and {@code after}. */
    List<String> keys(ReviewPane pane, Focus focus, List<String> before, List<String> after) {
        if (submitPrompt.isPresent()) {
            return SUBMIT_KEYS;
        }
        if (pane.box().isPresent()) {
            return BOX_KEYS;
        }
        List<String> panel = switch (focus) {
            case MAIN -> DIFF_KEYS;
            case COMMENTS -> COMMENTS_KEYS;
            default -> FILES_KEYS;
        };
        return Stream.of(before, panel, after).flatMap(List::stream).toList();
    }

    /** The keys of the open review; {@code false} for one it leaves to the rest of the keys. */
    boolean press(ReviewPane pane, KeyEvent key) {
        return pane.box().map(box -> pressInBox(pane, box, key)).orElseGet(() -> pressOutsideBox(pane, key));
    }

    private boolean pressOutsideBox(ReviewPane pane, KeyEvent key) {
        if (key.isKey(KeyCode.ESCAPE)) {
            return pane.clearRange() || host.back().getAsBoolean();
        }
        if (key.hasAlt()) {
            return false;
        }
        if (key.hasCtrl()) {
            return !TerminalInputs.isFixed(key, FixedChord.QUIT) && host.focus().get() == Focus.MAIN && page(pane, key);
        }
        if (key.isChar(OPEN_REVIEW)) {
            return true;
        }
        if (key.isChar(SUMMARY)) {
            pane.openBox(new CommentBox(new SummaryTarget(), Optional.empty(), pane.summaryToSubmit().value()));
            return true;
        }
        if (key.isChar(RESOLVED)) {
            pane.toggleResolved();
            return true;
        }
        return switch (host.focus().get()) {
            case MAIN -> moveInDiff(pane, key) || writeInDiff(pane, key);
            case FILES -> moveInFiles(pane, key);
            case COMMENTS -> moveInComments(pane, key);
            default -> false;
        };
    }

    /** The confirm after the summary is saved: {@code y} submits the pending review as a Comment, any other key leaves it pending; none while no confirm waits. */
    Optional<Boolean> answerSubmitPrompt(KeyEvent key) {
        return submitPrompt.map(pane -> answerSubmitPrompt(pane, key));
    }

    private boolean answerSubmitPrompt(ReviewPane pane, KeyEvent key) {
        submitPrompt = Optional.empty();
        if (key.isChar(YES) && !key.hasCtrl() && !key.hasAlt()) {
            PendingSubmitPayload submit = new PendingSubmitPayload(root, pane.number(), pane.summaryToSubmit());
            inReviewOrder(pane, () -> writing.submit(submit), answer -> {
                switch (answer) {
                    case WrittenReviewView _ -> host.submitted().run();
                    case RefusedWriteView refused -> host.refuse().accept(NOT_SUBMITTED.formatted(refused.reason().value()));
                }
            });
        }
        return true;
    }

    /** {@code c} on the cursor's line, or on the range marked, opens the comment box under it. */
    private boolean writeInDiff(ReviewPane pane, KeyEvent key) {
        if (key.isChar(RANGE)) {
            pane.markRange().ifPresent(reason -> host.refuse().accept(reason.value()));
            return true;
        }
        if (key.isKey(KeyCode.ENTER) || key.isChar(DELETE)) {
            pane.pendingOnCursorLine().ifPresent(comment -> editOrDelete(pane, key, comment, Optional.of(pane.cursor())));
            return true;
        }
        if (!key.isChar(COMMENT)) {
            return false;
        }
        if (pane.rows().hasLines()) {
            switch (pane.anchor()) {
                case OnDiffAnchorView anchor ->
                    pane.openBox(new CommentBox(new NewCommentTarget(anchor), Optional.of(pane.anchorLine())));
                case RefusedAnchorView refused -> host.refuse().accept(refused.reason().value());
            }
        }
        return true;
    }

    /** {@code enter} opens a pending comment in the box, under {@code inlineAt} if it has a line; {@code x} deletes it. */
    private void editOrDelete(ReviewPane pane, KeyEvent key, PendingCommentView comment, Optional<Integer> inlineAt) {
        if (key.isKey(KeyCode.ENTER)) {
            pane.openBox(new CommentBox(new EditCommentTarget(comment), inlineAt, comment.body().value()));
        } else {
            inReviewOrder(
                    pane,
                    () -> writing.delete(new PendingDeletePayload(root, pane.number(), comment.id())),
                    answer -> {
                        switch (answer) {
                            case WrittenReviewView written -> pane.applyWritten(written.pending());
                            case RefusedWriteView refused -> host.refuse().accept(NOT_DELETED.formatted(refused.reason().value()));
                        }
                    });
        }
    }

    /** The box takes every key: {@code ctrl+s} saves, {@code esc} throws it away, the rest edit its text; only {@code ctrl+c} still quits. */
    private boolean pressInBox(ReviewPane pane, CommentBox box, KeyEvent key) {
        if (TerminalInputs.isFixed(key, FixedChord.QUIT)) {
            return false;
        }
        if (box.saving()) {
            return true;
        }
        if (key.isKey(KeyCode.ESCAPE)) {
            pane.closeBox();
        } else if (key.hasCtrl() && key.isChar(SAVE)) {
            saveBox(pane, box);
        } else {
            box.edit(key);
        }
        return true;
    }

    /** Saves what the box holds: the summary is kept here until the submit, anything else is written off the render thread. */
    private void saveBox(ReviewPane pane, CommentBox box) {
        PendingReviewCommand command = writing;
        Path project = root;
        PullRequestNumber number = pane.number();
        String content = box.content();
        switch (box.target()) {
            case NewCommentTarget target ->
                write(pane, box, () -> command.comment(new PendingCommentPayload(
                        project, number, target.anchor().path(), target.anchor().position(), new CommentBody(content))));
            case EditCommentTarget target ->
                write(pane, box, () -> command.edit(new PendingEditPayload(project, number, target.comment().id(), new CommentBody(content))));
            case ReplyTarget target ->
                write(pane, box, () -> command.reply(new PendingReplyPayload(project, number, target.thread().id(), new CommentBody(content))));
            case SummaryTarget _ -> holdSummary(pane, content);
        }
    }

    /** The summary writes nothing: GitHub refuses to edit a pending review with no body, so it travels with the submit, and the confirm is asked at once. */
    private void holdSummary(ReviewPane pane, String content) {
        pane.typeSummary(new Summary(content));
        pane.closeBox();
        submitPrompt = Optional.of(pane);
    }

    /** Writes off the render thread; GitHub's answer closes the box, or keeps it with the reason it was refused. */
    private void write(ReviewPane pane, CommentBox box, Supplier<PendingReviewWriteView> write) {
        box.saveBegun();
        inReviewOrder(pane, write, answer -> {
            switch (answer) {
                case WrittenReviewView written -> {
                    pane.applyWritten(written.pending());
                    pane.closeBox();
                }
                case RefusedWriteView refused -> box.saveRefused(refused.reason());
            }
        });
    }

    /** Writes to the pending review one after the other, off the render thread; an answer for a review since left is dropped. */
    private void inReviewOrder(ReviewPane pane, Supplier<PendingReviewWriteView> write, Consumer<PendingReviewWriteView> answered) {
        writes = writes
                .exceptionally(_ -> null)
                .thenApplyAsync(_ -> write.get(), background)
                .exceptionally(ReviewReads::refusedWrite)
                .thenAccept(answer -> host.onRenderThread().accept(() -> landed(pane, () -> answered.accept(answer))));
    }

    private static boolean page(ReviewPane pane, KeyEvent key) {
        if (TerminalInputs.isFixed(key, FixedChord.HALF_PAGE_DOWN)) {
            pane.halfPageDown();
            return true;
        }
        if (TerminalInputs.isFixed(key, FixedChord.HALF_PAGE_UP)) {
            pane.halfPageUp();
            return true;
        }
        return false;
    }

    private static boolean moveInDiff(ReviewPane pane, KeyEvent key) {
        if (key.isChar(DOWN)) {
            pane.lineDown();
        } else if (key.isChar(UP)) {
            pane.lineUp();
        } else if (key.isChar(NEXT_FILE)) {
            pane.nextFile();
        } else if (key.isChar(PREVIOUS_FILE)) {
            pane.previousFile();
        } else {
            return false;
        }
        return true;
    }

    private static boolean moveInFiles(ReviewPane pane, KeyEvent key) {
        if (key.isChar(DOWN)) {
            pane.selectFileDown();
        } else if (key.isChar(UP)) {
            pane.selectFileUp();
        } else if (key.isKey(KeyCode.ENTER)) {
            pane.goToSelectedFile();
        } else {
            return false;
        }
        return true;
    }

    private boolean moveInComments(ReviewPane pane, KeyEvent key) {
        if (key.isChar(DOWN)) {
            pane.selectCommentDown();
        } else if (key.isChar(UP)) {
            pane.selectCommentUp();
        } else if (key.isKey(KeyCode.ENTER) || key.isChar(DELETE) || key.isChar(COMMENT)) {
            actOnSelectedComment(pane, key);
        } else {
            return false;
        }
        return true;
    }

    /** {@code enter} goes to a thread's line or opens a pending comment, {@code x} deletes a pending comment and {@code c} replies to a thread. */
    private void actOnSelectedComment(ReviewPane pane, KeyEvent key) {
        pane.selectedEntry().ifPresent(entry -> {
            switch (entry) {
                case PendingEntry pending when !key.isChar(COMMENT) -> {
                    pane.goToSelectedComment();
                    editOrDelete(pane, key, pending.placed().comment(), pane.lineOf(pending.placed().line()));
                }
                case ThreadEntry thread when key.isChar(COMMENT) ->
                    pane.openBox(new CommentBox(
                            new ReplyTarget(thread.placed().thread()), pane.lineOfThread(thread.placed().thread())));
                case ThreadEntry _ when key.isKey(KeyCode.ENTER) -> pane.goToSelectedComment();
                default -> {}
            }
        });
    }

    private void landed(ReviewPane pane, Runnable apply) {
        if (host.showing().test(pane)) {
            apply.run();
        }
    }
}
