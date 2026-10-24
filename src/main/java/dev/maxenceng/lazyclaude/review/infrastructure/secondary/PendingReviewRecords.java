package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.domain.NoPendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewId;
import dev.maxenceng.lazyclaude.review.domain.PullRequestNodeId;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewInProgress;
import dev.maxenceng.lazyclaude.review.domain.ReviewedPullRequest;
import dev.maxenceng.lazyclaude.review.published.CommitSha;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** Reads the pages {@code gh api graphql --paginate} writes for the pending review query into the pull request and your review of it, through {@link GhJson}. */
final class PendingReviewRecords {

    private static final String ID = "pull request id";
    private static final String HEAD = "head commit";

    private PendingReviewRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static ReviewedPullRequest pullRequest(byte[] json, String source) {
        List<ThreadPagePullRequestRecord> pages = GhJson.pages(json, source).stream()
                .map(page -> pullRequestOf(page).orElseThrow(() -> new PullRequestsUnavailableException("%s holds no pull request".formatted(source))))
                .toList();
        ThreadPagePullRequestRecord first = complete(
                pages.stream()
                        .findFirst()
                        .orElseThrow(() -> new PullRequestsUnavailableException("%s printed no pull request".formatted(source))),
                source);
        List<PendingComment> comments = pages.stream()
                .flatMap(page -> page.reviewThreads().nodes().stream())
                .map(thread -> complete(thread, source))
                .flatMap(thread -> thread.pendingComments().stream())
                .toList();
        return ReviewedPullRequest.builder()
                .id(new PullRequestNodeId(first.id()))
                .head(new CommitSha(first.headRefOid()))
                .review(reviewOf(first, comments))
                .build();
    }

    /** @throws PullRequestsUnavailableException naming the fields {@code source} left out of the pull request or its pending review */
    private static ThreadPagePullRequestRecord complete(ThreadPagePullRequestRecord pullRequest, String source) {
        MissingFields missing = review(pullRequest).stream()
                .map(PendingReviewRecord::missingFields)
                .reduce(new MissingFields().blank(ID, pullRequest.id()).blank(HEAD, pullRequest.headRefOid()), MissingFields::and);
        if (missing.any()) {
            throw new PullRequestsUnavailableException("%s wrote a pull request without %s".formatted(source, missing.listed()));
        }
        return pullRequest;
    }

    /** @throws PullRequestsUnavailableException naming the fields {@code source} left out of a thread holding pending comments */
    private static ThreadRecord complete(ThreadRecord thread, String source) {
        MissingFields missing = thread.missingFields();
        MissingFields all = missing.any() ? missing : missing.and(thread.missingPendingFields());
        if (all.any()) {
            throw new PullRequestsUnavailableException("%s wrote a review thread without %s".formatted(source, all.listed()));
        }
        return thread;
    }

    private static ReviewInProgress reviewOf(ThreadPagePullRequestRecord pullRequest, List<PendingComment> comments) {
        return review(pullRequest)
                .<ReviewInProgress>map(review -> PendingReview.builder()
                        .id(new PendingReviewId(review.id()))
                        .writtenOn(new CommitSha(review.commit().oid()))
                        .summary(new Summary(review.body()))
                        .comments(comments)
                        .build())
                .orElseGet(NoPendingReview::new);
    }

    private static Optional<PendingReviewRecord> review(ThreadPagePullRequestRecord pullRequest) {
        return Optional.ofNullable(pullRequest.reviews()).flatMap(reviews -> reviews.nodes().stream().findFirst());
    }

    private static Optional<ThreadPagePullRequestRecord> pullRequestOf(ThreadPageRecord page) {
        return Optional.ofNullable(page.data())
                .map(ThreadPageDataRecord::repository)
                .map(ThreadPageRepositoryRecord::pullRequest)
                .filter(pullRequest -> pullRequest.reviewThreads() != null);
    }
}
