package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.HeadCommit;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;

/** One merged pull request as {@code gh pr list --json} writes it, null where the JSON lacks it. */
record MergedPullRequestRecord(Integer number, String headRefName, String headRefOid, Boolean isCrossRepository) {

    boolean fromAFork() {
        return Boolean.TRUE.equals(isCrossRepository);
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field the domain needs is missing or refused */
    MergedPullRequest toMergedPullRequest() {
        Assert.notNull("pull request number", number);
        return MergedPullRequest.builder()
                .number(new PullRequestNumber(number))
                .head(new HeadBranch(headRefName))
                .headCommit(new HeadCommit(headRefOid))
                .build();
    }
}
