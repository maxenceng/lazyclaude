package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** GitHub took the write; {@code pending} is the pending review read again after it, which is what it is then, whether it could be read or not. */
public record WrittenReviewView(PendingReviewView pending) implements PendingReviewWriteView {}
