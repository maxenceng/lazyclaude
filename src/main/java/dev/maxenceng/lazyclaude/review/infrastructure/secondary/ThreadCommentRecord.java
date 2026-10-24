package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.review.published.CommentAuthor;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One comment of a thread as the threads query writes it, null where the JSON lacks it. */
record ThreadCommentRecord(String id, ThreadAuthorRecord author, String state, String body) {

    /** What GitHub calls the author of a comment whose account is gone. */
    private static final String GHOST = "ghost";

    /** What GitHub calls the state of a comment of a review that has not been submitted. */
    private static final String PENDING = "PENDING";

    private static final String ID = "comment id";
    private static final String BODY = "comment body";

    /** Whether the comment belongs to a pending review, which is shown as that and never as part of a thread. */
    boolean pending() {
        return PENDING.equals(state);
    }

    /** A comment lacks its body when the JSON has none. */
    MissingFields missingFields() {
        return new MissingFields().absent(BODY, body);
    }

    /** A pending comment lacks its id when the JSON has none, which editing it needs; only the pending review query asks for it. */
    MissingFields missingPendingFields() {
        return pending() ? new MissingFields().blank(ID, id) : new MissingFields();
    }

    /** Only once {@link #missingFields()} names none. */
    ThreadComment toComment() {
        return ThreadComment.builder()
                .author(new CommentAuthor(author == null || author.login() == null ? GHOST : author.login()))
                .body(new CommentBody(body))
                .build();
    }
}
