package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.Checks;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;

/** One pull request as {@code gh pr list --json} writes it, null where the JSON lacks it. */
record PullRequestRecord(
        Integer number,
        String title,
        String headRefName,
        Boolean isDraft,
        Boolean isCrossRepository,
        List<CheckRecord> statusCheckRollup) {

    boolean fromAFork() {
        return Boolean.TRUE.equals(isCrossRepository);
    }

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field the domain needs is missing or refused */
    PullRequest toPullRequest() {
        Assert.notNull("pull request number", number);
        return PullRequest.builder()
                .number(new PullRequestNumber(number))
                .title(new PullRequestTitle(title))
                .head(new HeadBranch(headRefName))
                .draft(Boolean.TRUE.equals(isDraft))
                .checks(new Checks(checks().stream().map(CheckRecord::toCheck).toList()))
                .build();
    }

    private List<CheckRecord> checks() {
        return statusCheckRollup == null ? List.of() : statusCheckRollup;
    }
}
