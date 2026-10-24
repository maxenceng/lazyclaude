package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
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

/** Reads the pages {@code gh api graphql --paginate} writes for the pending review query into the pull request and your review of it, through {@link GhJson}. */
final class PendingReviewRecords {

    private PendingReviewRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static ReviewedPullRequest pullRequest(byte[] json, String source) {
        List<ThreadPagePullRequestRecord> pages = GhJson.pages(json, ThreadPageRecord.class, source).stream()
                .map(page -> pullRequestOf(page).orElseThrow(() -> new PullRequestsUnavailableException("%s holds no pull request".formatted(source))))
                .toList();
        ThreadPagePullRequestRecord first = pages.stream()
                .findFirst()
                .orElseThrow(() -> new PullRequestsUnavailableException("%s printed no pull request".formatted(source)));
        try {
            List<PendingComment> comments = pages.stream()
                    .flatMap(page -> page.reviewThreads().nodes().stream())
                    .flatMap(thread -> thread.pendingComments().stream())
                    .toList();
            return ReviewedPullRequest.builder()
                    .id(new PullRequestNodeId(first.id()))
                    .head(new CommitSha(first.headRefOid()))
                    .review(reviewOf(first, comments))
                    .build();
        } catch (DomainException refused) {
            throw new PullRequestsUnavailableException(
                    "%s is not a pull request: %s".formatted(source, refused.getMessage()), refused);
        }
    }

    private static ReviewInProgress reviewOf(ThreadPagePullRequestRecord pullRequest, List<PendingComment> comments) {
        return Optional.ofNullable(pullRequest.reviews())
                .flatMap(reviews -> reviews.nodes().stream().findFirst())
                .<ReviewInProgress>map(review -> PendingReview.builder()
                        .id(new PendingReviewId(review.id()))
                        .writtenOn(new CommitSha(Optional.ofNullable(review.commit())
                                .map(PendingReviewCommitRecord::oid)
                                .orElseThrow(() -> new PullRequestsUnavailableException("the pending review names no commit"))))
                        .summary(new Summary(review.body()))
                        .comments(comments)
                        .build())
                .orElseGet(NoPendingReview::new);
    }

    private static Optional<ThreadPagePullRequestRecord> pullRequestOf(ThreadPageRecord page) {
        return Optional.ofNullable(page.data())
                .map(ThreadPageDataRecord::repository)
                .map(ThreadPageRepositoryRecord::pullRequest)
                .filter(pullRequest -> pullRequest.reviewThreads() != null);
    }
}
