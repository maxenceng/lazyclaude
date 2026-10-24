package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where your pending review of a pull request is read from and written to: GitHub, which keeps it until it is submitted. */
public interface PendingReviewPort {

    /**
     * Pull request {@code number} of {@code project}'s repository, with your pending review of it if there is one.
     *
     * @throws PullRequestsUnavailableException when GitHub cannot be read, with the reason
     */
    ReviewedPullRequest pullRequest(Project project, PullRequestNumber number);

    /**
     * Starts your pending review of the pull request, with no comment and no summary.
     *
     * @throws PullRequestsUnavailableException when GitHub does not take it, with the reason
     */
    PendingReviewId create(Project project, PullRequestNodeId pullRequest);

    /** @throws PullRequestsUnavailableException when GitHub does not take the comment, with the reason */
    void addThread(Project project, PendingReviewId review, PendingOnDiff where, CommentBody body);

    /** @throws PullRequestsUnavailableException when GitHub does not take the reply, with the reason */
    void reply(Project project, PendingReviewId review, ThreadId thread, CommentBody body);

    /** @throws PullRequestsUnavailableException when GitHub does not take the new text, with the reason */
    void edit(Project project, PendingCommentId comment, CommentBody body);

    /** @throws PullRequestsUnavailableException when GitHub does not delete the comment, with the reason */
    void delete(Project project, PendingCommentId comment);

    /**
     * Sends the pending review as a Comment review with {@code summary} as its body, which GitHub takes only now: it refuses to edit
     * the body of a pending review that has none.
     *
     * @throws PullRequestsUnavailableException when GitHub refuses it or cannot be reached, with the reason
     */
    void submit(Project project, PendingReviewId review, Summary summary);
}
