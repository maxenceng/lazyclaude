package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewId;
import dev.maxenceng.lazyclaude.review.domain.PendingReviewPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestNodeId;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewedPullRequest;
import dev.maxenceng.lazyclaude.review.published.CommentBody;
import dev.maxenceng.lazyclaude.review.published.PendingCommentId;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Summary;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads and writes your pending review of a pull request through {@code gh api graphql}, the one place GitHub offers all of it (ADR 0009).
 * One query reads the pull request's id and head, your pending review and, through the review threads, where its comments hang; a
 * mutation writes each change. Any way {@code gh} fails to answer is the one unavailable reason, in the words {@code gh} used where it used any.
 */
public final class GhPendingReviewRepository implements PendingReviewPort {

    // deferred: only a thread's first 50 comments are asked for, so a pending reply after the fiftieth is not seen — close it when a thread shows that many.
    private static final String QUERY = """
            query($owner: String!, $repo: String!, $number: Int!, $endCursor: String) {
              repository(owner: $owner, name: $repo) {
                pullRequest(number: $number) {
                  id
                  headRefOid
                  reviews(states: PENDING, first: 1) { nodes { id body commit { oid } } }
                  reviewThreads(first: 100, after: $endCursor) {
                    pageInfo { hasNextPage endCursor }
                    nodes {
                      id path line originalLine startLine originalStartLine diffSide isOutdated
                      comments(first: 50) { nodes { id state body } }
                    }
                  }
                }
              }
            }
            """;

    private static final String CREATE = """
            mutation($pullRequestId: ID!) {
              addPullRequestReview(input: {pullRequestId: $pullRequestId}) { pullRequestReview { id } }
            }
            """;
    private static final String ADD_THREAD = """
            mutation($review: ID!, $path: String!, $body: String!, $line: Int!, $side: DiffSide!, $startLine: Int, $startSide: DiffSide) {
              addPullRequestReviewThread(input: {
                pullRequestReviewId: $review, path: $path, body: $body,
                line: $line, side: $side, startLine: $startLine, startSide: $startSide
              }) { thread { id } }
            }
            """;
    private static final String REPLY = """
            mutation($review: ID!, $thread: ID!, $body: String!) {
              addPullRequestReviewThreadReply(input: {
                pullRequestReviewId: $review, pullRequestReviewThreadId: $thread, body: $body
              }) { comment { id } }
            }
            """;
    private static final String EDIT = """
            mutation($comment: ID!, $body: String!) {
              updatePullRequestReviewComment(input: {pullRequestReviewCommentId: $comment, body: $body}) { pullRequestReviewComment { id } }
            }
            """;
    private static final String DELETE = """
            mutation($comment: ID!) {
              deletePullRequestReviewComment(input: {id: $comment}) { pullRequestReview { id } }
            }
            """;
    private static final String SUBMIT = """
            mutation($review: ID!, $body: String!) {
              submitPullRequestReview(input: {pullRequestReviewId: $review, event: COMMENT, body: $body}) { pullRequestReview { id } }
            }
            """;
    private static final String CREATED_ID = ".data.addPullRequestReview.pullRequestReview.id";
    private static final String OWNER = "owner";
    private static final String THIS_OWNER = "{owner}";
    private static final String REPO = "repo";
    private static final String THIS_REPO = "{repo}";
    private static final String NUMBER = "number";
    private static final String PULL_REQUEST_ID = "pullRequestId";
    private static final String REVIEW = "review";
    private static final String THREAD = "thread";
    private static final String COMMENT = "comment";
    private static final String PATH = "path";
    private static final String BODY = "body";
    private static final String LINE = "line";
    private static final String SIDE = "side";
    private static final String START_LINE = "startLine";
    private static final String START_SIDE = "startSide";

    private final GhCommand gh;

    public GhPendingReviewRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public ReviewedPullRequest pullRequest(Project project, PullRequestNumber number) {
        List<String> arguments = new GraphqlCall()
                .paginated()
                .typed(OWNER, THIS_OWNER)
                .typed(REPO, THIS_REPO)
                .typed(NUMBER, number.value())
                .query(QUERY)
                .arguments();
        return PendingReviewRecords.pullRequest(gh.output(project, arguments), GhCommand.sourceOf(arguments));
    }

    @Override
    public PendingReviewId create(Project project, PullRequestNodeId pullRequest) {
        List<String> arguments = mutation(CREATE).text(PULL_REQUEST_ID, pullRequest.value()).picking(CREATED_ID).arguments();
        String printed = new String(gh.output(project, arguments), StandardCharsets.UTF_8).strip();
        if (printed.isEmpty()) {
            throw new PullRequestsUnavailableException("%s printed no review id".formatted(GhCommand.sourceOf(arguments)));
        }
        return new PendingReviewId(printed);
    }

    @Override
    public void comment(Project project, PendingReviewId review, PendingOnDiff where, CommentBody body) {
        PlacedThread position = where.position();
        GraphqlCall onLast = mutation(ADD_THREAD)
                .text(REVIEW, review.value())
                .text(PATH, where.path().value())
                .text(BODY, body.value())
                .typed(LINE, position.last().value())
                .text(SIDE, position.side().name());
        GraphqlCall call = position.first().equals(position.last())
                ? onLast
                : onLast.typed(START_LINE, position.first().value()).text(START_SIDE, position.side().name());
        run(project, call.arguments());
    }

    @Override
    public void reply(Project project, PendingReviewId review, ThreadId thread, CommentBody body) {
        run(project, mutation(REPLY).text(REVIEW, review.value()).text(THREAD, thread.value()).text(BODY, body.value()).arguments());
    }

    @Override
    public void edit(Project project, PendingCommentId comment, CommentBody body) {
        run(project, mutation(EDIT).text(COMMENT, comment.value()).text(BODY, body.value()).arguments());
    }

    @Override
    public void delete(Project project, PendingCommentId comment) {
        run(project, mutation(DELETE).text(COMMENT, comment.value()).arguments());
    }

    @Override
    public void submit(Project project, PendingReviewId review, Summary summary) {
        run(project, mutation(SUBMIT).text(REVIEW, review.value()).text(BODY, summary.value()).arguments());
    }

    /** A {@code gh api graphql} call that runs {@code mutation}, its variables still to add. */
    private static GraphqlCall mutation(String mutation) {
        return new GraphqlCall().query(mutation);
    }

    private void run(Project project, List<String> arguments) {
        gh.output(project, arguments);
    }
}
