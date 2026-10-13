package dev.maxenceng.lazyclaude.review.published;

/** Where one check stands, worst first: declaration order is both the order checks are listed in and the CI result's precedence. */
public enum CheckOutcome {
    FAILED,
    PENDING,
    PASSED
}
