package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.Checks;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One pull request as {@code gh pr list --json} writes it, null where the JSON lacks it. */
record PullRequestRecord(
        Integer number,
        String title,
        String headRefName,
        Boolean isDraft,
        Boolean isCrossRepository,
        List<CheckRecord> statusCheckRollup) {

    private static final String NUMBER = "number";
    private static final String TITLE = "title";
    private static final String HEAD = "head branch";

    boolean fromAFork() {
        return Boolean.TRUE.equals(isCrossRepository);
    }

    /** The fields a pull request cannot be read without that the JSON lacks, its checks' included. */
    MissingFields missingFields() {
        return checks().stream()
                .map(CheckRecord::missingFields)
                .reduce(new MissingFields().absent(NUMBER, number).blank(TITLE, title).blank(HEAD, headRefName), MissingFields::and);
    }

    /** Only once {@link #missingFields()} names none. */
    PullRequest toPullRequest() {
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
