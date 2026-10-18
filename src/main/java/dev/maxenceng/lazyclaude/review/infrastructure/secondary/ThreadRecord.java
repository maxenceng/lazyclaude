package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.PendingAnchor;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingReply;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.OutdatedThread;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/** One review thread as the threads query writes it, null where the JSON lacks it. */
record ThreadRecord(
        String id,
        String path,
        Integer line,
        Integer originalLine,
        Integer startLine,
        Integer originalStartLine,
        String diffSide,
        Boolean isResolved,
        Boolean isOutdated,
        ThreadCommentConnectionRecord comments) {

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field the domain needs is missing or refused */
    ReviewThread toThread() {
        return ReviewThread.builder()
                .id(new ThreadId(id))
                .path(new DiffPath(path))
                .position(position())
                .resolved(Boolean.TRUE.equals(isResolved))
                .comments(submittedComments().map(ThreadCommentRecord::toComment).toList())
                .build();
    }

    /** Whether anything but pending comments is said in the thread; a thread of nothing else is shown as pending comments, not as a thread. */
    boolean hasSubmittedComments() {
        return submittedComments().findAny().isPresent();
    }

    private Stream<ThreadCommentRecord> submittedComments() {
        Assert.notNull("thread comments", comments);
        return comments.nodes().stream().filter(comment -> !comment.pending());
    }

    /**
     * The pending comments of the thread: the one that starts it is a comment on lines of the diff, and any other a reply to the thread.
     * A pending comment on a whole file names no line, and is left out.
     */
    // deferred: a pending comment on a whole file, made on github.com, is left out of the pending review — close it when one is seen.
    List<PendingComment> pendingComments() {
        return IntStream.range(0, comments.nodes().size())
                .filter(index -> comments.nodes().get(index).pending())
                .mapToObj(index -> pendingComment(comments.nodes().get(index), index == 0))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<PendingComment> pendingComment(ThreadCommentRecord comment, boolean startsThread) {
        return (startsThread ? onDiff().<PendingAnchor>map(anchor -> anchor) : Optional.<PendingAnchor>of(reply()))
                .map(anchor -> PendingComment.builder()
                        .id(new PendingCommentId(comment.id()))
                        .body(new CommentBody(comment.body()))
                        .anchor(anchor)
                        .build());
    }

    /** A comment starting a thread sits on its line; one on a line the pull request no longer has sits on the line it was written on. */
    private Optional<PendingOnDiff> onDiff() {
        Integer last = line == null ? originalLine : line;
        Integer first = line == null ? originalStartLine : startLine;
        return Optional.ofNullable(last)
                .map(end -> PendingOnDiff.builder()
                        .path(new DiffPath(path))
                        .position(PlacedThread.builder()
                                .side(side())
                                .first(new DiffLineNumber(first == null ? end : first))
                                .last(new DiffLineNumber(end))
                                .build())
                        .build());
    }

    private PendingReply reply() {
        return PendingReply.builder()
                .thread(new ThreadId(id))
                .path(new DiffPath(path))
                .position(position())
                .build();
    }

    /** A thread GitHub marks outdated, or that names no line, sits on none; a range runs from its start line to its line. */
    private ThreadPosition position() {
        if (Boolean.TRUE.equals(isOutdated) || line == null) {
            return new OutdatedThread();
        }
        return PlacedThread.builder()
                .side(side())
                .first(new DiffLineNumber(startLine == null ? line : startLine))
                .last(new DiffLineNumber(line))
                .build();
    }

    private ThreadSide side() {
        List<ThreadSide> sides = List.of(ThreadSide.values());
        return sides.stream()
                .filter(side -> side.name().equals(diffSide))
                .findFirst()
                .orElseThrow(() -> new PullRequestsUnavailableException("a review thread sits on the side %s".formatted(diffSide)));
    }
}
