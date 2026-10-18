package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

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
    private static final String SUMMARISE = """
            mutation($review: ID!, $body: String!) {
              updatePullRequestReview(input: {pullRequestReviewId: $review, body: $body}) { pullRequestReview { id } }
            }
            """;
    private static final String SUBMIT = """
            mutation($review: ID!) {
              submitPullRequestReview(input: {pullRequestReviewId: $review, event: COMMENT}) { pullRequestReview { id } }
            }
            """;
    private static final String CREATED_ID = ".data.addPullRequestReview.pullRequestReview.id";

    private final GhCommand gh;

    public GhPendingReviewRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public ReviewedPullRequest pullRequest(Project project, PullRequestNumber number) {
        List<String> arguments = List.of(
                "api", "graphql", "--paginate",
                "-F", "owner={owner}",
                "-F", "repo={repo}",
                "-F", "number=%d".formatted(number.value()),
                "-f", "query=" + QUERY);
        return PendingReviewRecords.pullRequest(gh.output(project, arguments), GhCommand.sourceOf(arguments));
    }

    @Override
    public PendingReviewId create(Project project, PullRequestNodeId pullRequest) {
        List<String> arguments = mutation(CREATE, List.of("-f", "pullRequestId=" + pullRequest.value(), "--jq", CREATED_ID));
        String printed = new String(gh.output(project, arguments), StandardCharsets.UTF_8).strip();
        if (printed.isEmpty()) {
            throw new PullRequestsUnavailableException("%s printed no review id".formatted(GhCommand.sourceOf(arguments)));
        }
        return new PendingReviewId(printed);
    }

    @Override
    public void addThread(Project project, PendingReviewId review, PendingOnDiff where, CommentBody body) {
        PlacedThread position = where.position();
        List<String> range = position.first().equals(position.last())
                ? List.of()
                : List.of("-F", "startLine=%d".formatted(position.first().value()), "-f", "startSide=" + position.side().name());
        run(project, mutation(
                ADD_THREAD,
                Stream.concat(
                                Stream.of(
                                        "-f", "review=" + review.value(),
                                        "-f", "path=" + where.path().value(),
                                        "-f", "body=" + body.value(),
                                        "-F", "line=%d".formatted(position.last().value()),
                                        "-f", "side=" + position.side().name()),
                                range.stream())
                        .toList()));
    }

    @Override
    public void reply(Project project, PendingReviewId review, ThreadId thread, CommentBody body) {
        run(project, mutation(REPLY, List.of("-f", "review=" + review.value(), "-f", "thread=" + thread.value(), "-f", "body=" + body.value())));
    }

    @Override
    public void edit(Project project, PendingCommentId comment, CommentBody body) {
        run(project, mutation(EDIT, List.of("-f", "comment=" + comment.value(), "-f", "body=" + body.value())));
    }

    @Override
    public void delete(Project project, PendingCommentId comment) {
        run(project, mutation(DELETE, List.of("-f", "comment=" + comment.value())));
    }

    @Override
    public void summarise(Project project, PendingReviewId review, Summary summary) {
        run(project, mutation(SUMMARISE, List.of("-f", "review=" + review.value(), "-f", "body=" + summary.value())));
    }

    @Override
    public void submit(Project project, PendingReviewId review) {
        run(project, mutation(SUBMIT, List.of("-f", "review=" + review.value())));
    }

    /** The arguments of a {@code gh api graphql} call that runs {@code mutation} with {@code variables}, each given as {@code -f name=value} or {@code -F name=number}. */
    private static List<String> mutation(String mutation, List<String> variables) {
        return Stream.concat(Stream.of("api", "graphql", "-f", "query=" + mutation), variables.stream()).toList();
    }

    private void run(Project project, List<String> arguments) {
        gh.output(project, arguments);
    }
}
