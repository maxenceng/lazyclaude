package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingReviewWrite;
import dev.maxenceng.lazyclaude.review.domain.RefusedWrite;
import dev.maxenceng.lazyclaude.review.domain.WrittenReview;

/** What a write to the pending review came to: GitHub took it, or it did not. */
public sealed interface PendingReviewWriteView permits WrittenReviewView, RefusedWriteView {

    static PendingReviewWriteView from(PendingReviewWrite write) {
        return switch (write) {
            case WrittenReview written -> new WrittenReviewView(PendingReviewView.from(written.pending()));
            case RefusedWrite refused -> new RefusedWriteView(refused.reason());
        };
    }
}
