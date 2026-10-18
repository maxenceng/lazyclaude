package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.RefusedWriteView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ThreadsView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailableDiffView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailablePendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.UnavailableThreadsView;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** What a review read that failed unexpectedly is shown as, so that the pane is never left loading for ever. */
final class ReviewReads {

    private ReviewReads() {}

    static PullRequestDiffView unreadableDiff(Throwable failed) {
        return UnavailableDiffView.builder().reason(reasonOf(failed)).tooLarge(false).build();
    }

    static ThreadsView unreadableThreads(Throwable failed) {
        return new UnavailableThreadsView(reasonOf(failed));
    }

    static PendingReviewView unreadablePending(Throwable failed) {
        return new UnavailablePendingReviewView(reasonOf(failed));
    }

    static PendingReviewWriteView refusedWrite(Throwable failed) {
        return new RefusedWriteView(reasonOf(failed));
    }

    private static Reason reasonOf(Throwable failed) {
        Throwable cause = failed instanceof CompletionException wrapped && wrapped.getCause() != null ? wrapped.getCause() : failed;
        return new Reason(Optional.ofNullable(cause.getMessage())
                .filter(message -> !message.isBlank())
                .orElseGet(() -> cause.getClass().getSimpleName()));
    }
}
