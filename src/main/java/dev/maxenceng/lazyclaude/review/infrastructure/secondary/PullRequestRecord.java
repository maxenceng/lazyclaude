package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.domain.Checks;
import dev.maxenceng.lazyclaude.review.domain.ClosesIssue;
import dev.maxenceng.lazyclaude.review.domain.ClosesNoIssue;
import dev.maxenceng.lazyclaude.review.domain.CommentCount;
import dev.maxenceng.lazyclaude.review.domain.IssueClosing;
import dev.maxenceng.lazyclaude.review.domain.OpenedAt;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.PullRequestTitle;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** One pull request as {@code gh api graphql} writes it for the open pull requests query, null where the JSON lacks it. */
record PullRequestRecord(
        Integer number,
        String title,
        String headRefName,
        Boolean isDraft,
        Boolean isCrossRepository,
        String createdAt,
        RepositoryRecord baseRepository,
        CommentCountRecord comments,
        ReviewThreadCountsRecord reviewThreads,
        PendingReviewCountsRecord reviews,
        ClosingIssuesRecord closingIssuesReferences,
        CommitsRecord commits) {

    private static final String NUMBER = "number";
    private static final String TITLE = "title";
    private static final String HEAD = "head branch";
    private static final String CREATED_AT = "created at";
    private static final String REPOSITORY = "base repository";
    private static final String COMMENTS = "comment count";
    private static final String THREADS = "review threads";
    private static final String THREAD_COMMENTS = "review thread comment count";
    private static final String PENDING_REVIEWS = "pending reviews";
    private static final String PENDING_COMMENTS = "pending review comment count";
    private static final String CLOSING_ISSUES = "closing issues";
    private static final String UNREAD_TIME = "gh api graphql wrote a createdAt that is not a time: %s";

    boolean fromAFork() {
        return Boolean.TRUE.equals(isCrossRepository);
    }

    /** The fields a pull request cannot be read without that the JSON lacks, its checks', threads' and closed issues' included. */
    MissingFields missingFields() {
        MissingFields own = new MissingFields()
                .absent(NUMBER, number)
                .blank(TITLE, title)
                .blank(HEAD, headRefName)
                .blank(CREATED_AT, createdAt)
                .absent(REPOSITORY, baseRepository)
                .absent(COMMENTS, comments)
                .absent(THREADS, reviewThreads == null ? null : reviewThreads.nodes())
                .absent(PENDING_REVIEWS, reviews == null ? null : reviews.nodes())
                .absent(CLOSING_ISSUES, closingIssuesReferences == null ? null : closingIssuesReferences.nodes());
        return Stream.of(
                        Stream.ofNullable(baseRepository).map(repository -> repository.missingFields(REPOSITORY)),
                        Stream.ofNullable(comments).map(count -> count.missingFields(COMMENTS)),
                        threads().stream().map(ThreadCommentCountRecord::missingFields),
                        pendingReviews().stream().map(PendingReviewCountRecord::missingFields),
                        checks().stream().map(CheckRecord::missingFields),
                        closingIssues().stream().map(ClosingIssueRecord::missingFields))
                .flatMap(missing -> missing)
                .reduce(own, MissingFields::and);
    }

    /** Only once {@link #missingFields()} names none. */
    PullRequest toPullRequest() {
        return PullRequest.builder()
                .number(new PullRequestNumber(number))
                .title(new PullRequestTitle(title))
                .head(new HeadBranch(headRefName))
                .draft(Boolean.TRUE.equals(isDraft))
                .openedAt(new OpenedAt(openedTime()))
                .checks(new Checks(checks().stream().map(CheckRecord::toCheck).toList()))
                .comments(new CommentCount(visibleComments()))
                .pendingReview(!pendingReviews().isEmpty())
                .closing(closing())
                .build();
    }

    private Instant openedTime() {
        try {
            return Instant.parse(createdAt);
        } catch (DateTimeParseException e) {
            throw new PullRequestsUnavailableException(UNREAD_TIME.formatted(createdAt), e);
        }
    }

    // deferred: only the first 100 review threads are counted, so a pull request with more shows fewer comments — close it when one has that many.
    private int visibleComments() {
        int inThreads = threads().stream().mapToInt(ThreadCommentCountRecord::total).sum();
        int yours = pendingReviews().stream().mapToInt(PendingReviewCountRecord::total).sum();
        return comments.total() + inThreads - yours;
    }

    private IssueClosing closing() {
        return closingIssues().stream()
                .filter(issue -> issue.in(baseRepository))
                .findFirst()
                .<IssueClosing>map(issue -> new ClosesIssue(issue.toClosedIssueNumber()))
                .orElseGet(ClosesNoIssue::new);
    }

    private List<ThreadCommentCountRecord> threads() {
        return reviewThreads == null || reviewThreads.nodes() == null ? List.of() : reviewThreads.nodes();
    }

    private List<PendingReviewCountRecord> pendingReviews() {
        return reviews == null || reviews.nodes() == null ? List.of() : reviews.nodes();
    }

    private List<ClosingIssueRecord> closingIssues() {
        return closingIssuesReferences == null || closingIssuesReferences.nodes() == null ? List.of() : closingIssuesReferences.nodes();
    }

    private List<CheckRecord> checks() {
        return commits == null ? List.of() : commits.checks();
    }
}
