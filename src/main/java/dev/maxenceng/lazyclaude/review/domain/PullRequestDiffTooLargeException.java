package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.DomainErrorStatus;
import dev.maxenceng.lazyclaude.error.domain.DomainException;

/** GitHub refused to produce the diff because it is too large; the message says what GitHub said, for the person reading the screen. */
public final class PullRequestDiffTooLargeException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PullRequestDiffTooLargeException(String reason, Throwable cause) {
        super(DomainErrorStatus.UNAVAILABLE, reason, cause);
    }
}
