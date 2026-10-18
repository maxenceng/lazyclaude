package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.DiffReading;
import dev.maxenceng.lazyclaude.review.domain.LoadingDiff;
import dev.maxenceng.lazyclaude.review.domain.LoadingThreads;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiff;
import dev.maxenceng.lazyclaude.review.domain.ReadDiff;
import dev.maxenceng.lazyclaude.review.domain.ReadThreads;
import dev.maxenceng.lazyclaude.review.domain.ThreadsReading;
import dev.maxenceng.lazyclaude.review.domain.UnavailableDiff;
import dev.maxenceng.lazyclaude.review.domain.UnavailableThreads;

/** The reads the shell holds as views, back as the readings they were made from, so that {@code review} can join them. */
final class ReviewReadings {

    private ReviewReadings() {}

    static DiffReading diffOf(PullRequestDiffView view) {
        return switch (view) {
            case LoadingDiffView _ -> new LoadingDiff();
            case ReadDiffView read -> new ReadDiff(new PullRequestDiff(read.files().stream().map(DiffFileView::toDomain).toList()));
            case UnavailableDiffView unavailable ->
                UnavailableDiff.builder().reason(unavailable.reason()).tooLarge(unavailable.tooLarge()).build();
        };
    }

    static ThreadsReading threadsOf(ThreadsView view) {
        return switch (view) {
            case LoadingThreadsView _ -> new LoadingThreads();
            case ReadThreadsView read -> new ReadThreads(read.threads().stream().map(ReviewThreadView::toDomain).toList());
            case UnavailableThreadsView unavailable -> new UnavailableThreads(unavailable.reason());
        };
    }

    /** The pending comments a read pending review holds; none until it is read, and none when it has none. */
    static List<PendingComment> commentsOf(PendingReviewView view) {
        return view instanceof OpenPendingReviewView open ? open.comments().stream().map(PendingCommentView::toDomain).toList() : List.of();
    }
}
