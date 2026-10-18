package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Asks {@code gh api graphql} for a pull request's review threads of the repository the project's directory belongs to, a page of 100
 * at a time until {@code gh} has paged through them. Any way {@code gh} fails to answer is the one unavailable reason, in the words
 * {@code gh} used where it used any.
 */
public final class GhThreadRepository implements ThreadPort {

    // deferred: only a thread's first 50 comments are asked for, so a longer conversation ends at its fiftieth — close it when a thread shows that many.
    private static final String QUERY = """
            query($owner: String!, $repo: String!, $number: Int!, $endCursor: String) {
              repository(owner: $owner, name: $repo) {
                pullRequest(number: $number) {
                  reviewThreads(first: 100, after: $endCursor) {
                    pageInfo { hasNextPage endCursor }
                    nodes {
                      path line startLine diffSide isResolved isOutdated
                      comments(first: 50) { nodes { author { login } body } }
                    }
                  }
                }
              }
            }
            """;

    private final GhCommand gh;

    public GhThreadRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public List<ReviewThread> threads(Project project, PullRequestNumber number) {
        List<String> arguments = List.of(
                "api", "graphql", "--paginate",
                "-F", "owner={owner}",
                "-F", "repo={repo}",
                "-F", "number=%d".formatted(number.value()),
                "-f", "query=" + QUERY);
        return ThreadRecords.threads(gh.output(project, arguments), GhCommand.sourceOf(arguments));
    }
}
