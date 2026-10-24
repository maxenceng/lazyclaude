package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.HeadCommit;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One merged pull request as {@code gh pr list --json} writes it, null where the JSON lacks it. */
record MergedPullRequestRecord(Integer number, String headRefName, String headRefOid, Boolean isCrossRepository) {

    private static final String NUMBER = "number";
    private static final String HEAD = "head branch";
    private static final String HEAD_COMMIT = "head commit";

    boolean fromAFork() {
        return Boolean.TRUE.equals(isCrossRepository);
    }

    /** The fields a merged pull request cannot be read without that the JSON lacks. */
    MissingFields missingFields() {
        return new MissingFields().absent(NUMBER, number).blank(HEAD, headRefName).blank(HEAD_COMMIT, headRefOid);
    }

    /** Only once {@link #missingFields()} names none. */
    MergedPullRequest toMergedPullRequest() {
        return MergedPullRequest.builder()
                .number(new PullRequestNumber(number))
                .head(new HeadBranch(headRefName))
                .headCommit(new HeadCommit(headRefOid))
                .build();
    }
}
