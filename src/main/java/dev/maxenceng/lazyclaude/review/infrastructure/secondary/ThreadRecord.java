package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.OutdatedThread;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/** One review thread as the threads query writes it, null where the JSON lacks it. */
record ThreadRecord(
        String path,
        Integer line,
        Integer startLine,
        String diffSide,
        Boolean isResolved,
        Boolean isOutdated,
        ThreadCommentConnectionRecord comments) {

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field the domain needs is missing or refused */
    ReviewThread toThread() {
        Assert.notNull("thread comments", comments);
        return ReviewThread.builder()
                .path(new DiffPath(path))
                .position(position())
                .resolved(Boolean.TRUE.equals(isResolved))
                .comments(comments.nodes().stream().map(ThreadCommentRecord::toComment).toList())
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
