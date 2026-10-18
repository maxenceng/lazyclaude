package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.LoadingPendingReview;
import dev.maxenceng.lazyclaude.review.domain.NoPendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewReading;
import dev.maxenceng.lazyclaude.review.domain.ReadPendingReview;
import dev.maxenceng.lazyclaude.review.domain.UnavailablePendingReview;

/** A pull request's pending review as the shell reads it: not read yet, there is none, the review itself, or why GitHub could not be asked for it. */
public sealed interface PendingReviewView
        permits LoadingPendingReviewView, NoPendingReviewView, OpenPendingReviewView, UnavailablePendingReviewView {

    static PendingReviewView from(PendingReviewReading reading) {
        return switch (reading) {
            case LoadingPendingReview _ -> new LoadingPendingReviewView();
            case ReadPendingReview read ->
                switch (read.pullRequest().review()) {
                    case PendingReview review -> OpenPendingReviewView.from(review, read.pullRequest().head());
                    case NoPendingReview _ -> new NoPendingReviewView();
                };
            case UnavailablePendingReview unavailable -> new UnavailablePendingReviewView(unavailable.reason());
        };
    }
}
