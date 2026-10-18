package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.diffPath;
import static dev.maxenceng.lazyclaude.review.published.DiffPathFixture.secondDiffPath;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.answer;
import static dev.maxenceng.lazyclaude.review.published.ThreadCommentFixture.question;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.outdated;
import static dev.maxenceng.lazyclaude.review.published.ThreadPositionFixture.rightOn;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReviewThread.ReviewThreadBuilder;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.OnDiffLine;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

public final class ReviewThreadFixture {

    private ReviewThreadFixture() {}

    public static ReviewThreadBuilder reviewThreadBuilder() {
        return ReviewThread.builder()
                .path(diffPath())
                .position(rightOn(3))
                .resolved(false)
                .comments(List.of(question(), answer()));
    }

    public static ReviewThread reviewThread() {
        return reviewThreadBuilder().build();
    }

    public static ReviewThread threadAt(DiffPath path, ThreadPosition position) {
        return reviewThreadBuilder().path(path).position(position).build();
    }

    public static ReviewThread threadOn(DiffPath path, ThreadPosition position, ThreadComment... comments) {
        return reviewThreadBuilder().path(path).position(position).comments(List.of(comments)).build();
    }

    public static ReviewThread resolvedThread() {
        return reviewThreadBuilder().resolved(true).build();
    }

    public static ReviewThread outdatedThread() {
        return reviewThreadBuilder().position(outdated()).build();
    }

    public static ReviewThread outdatedThreadIn(DiffPath path) {
        return reviewThreadBuilder().path(path).position(outdated()).build();
    }

    public static ReviewThread threadInSecondFile(ThreadPosition position) {
        return threadAt(secondDiffPath(), position);
    }

    public static ThreadInDiff onLine(ReviewThread thread, int index) {
        return ThreadInDiff.builder().thread(thread).line(new OnDiffLine(new DiffLineIndex(index))).build();
    }

    public static ThreadInDiff onNoLine(ReviewThread thread) {
        return ThreadInDiff.builder().thread(thread).line(new NoDiffLine()).build();
    }
}
