package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;

/** What {@link PendingReviewCommand} is asked a new comment on the diff with: the project, the pull request, the lines the comment hangs on and what it says. */
public record PendingCommentPayload(Path root, PullRequestNumber number, DiffPath path, PlacedThread position, CommentBody body) {

    PendingOnDiff where() {
        return PendingOnDiff.builder().path(path).position(position).build();
    }
}
