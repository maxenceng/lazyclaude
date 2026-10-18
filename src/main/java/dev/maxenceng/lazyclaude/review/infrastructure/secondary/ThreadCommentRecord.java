package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.review.published.CommentAuthor;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;

/** One comment of a thread as the threads query writes it, null where the JSON lacks it. */
record ThreadCommentRecord(ThreadAuthorRecord author, String state, String body) {

    /** What GitHub calls the author of a comment whose account is gone. */
    private static final String GHOST = "ghost";

    /** What GitHub calls the state of a comment of a review that has not been submitted. */
    private static final String PENDING = "PENDING";

    /** Whether the comment belongs to a pending review, which is shown as that and never as part of a thread. */
    boolean pending() {
        return PENDING.equals(state);
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when the comment has no body */
    ThreadComment toComment() {
        return ThreadComment.builder()
                .author(new CommentAuthor(author == null || author.login() == null ? GHOST : author.login()))
                .body(new CommentBody(body))
                .build();
    }
}
