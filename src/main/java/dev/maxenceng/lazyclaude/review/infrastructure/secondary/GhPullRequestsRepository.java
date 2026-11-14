package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Asks {@code gh api graphql} for the open pull requests of the repository the project's directory belongs to, with their checks, their
 * comments, whether you hold a pending review and the issue they close, in one call. A pull request from a fork is left out: its head
 * branch carries no owner, so it could shadow this repository's own. Any way {@code gh} fails to answer, a missing binary, a login it
 * lacks or a run it was killed in, is the one unavailable reason, in the words {@code gh} used where it used any.
 */
public final class GhPullRequestsRepository implements OpenPullRequestPort {

    // deferred: only the 100 newest open pull requests, a status rollup's first 100 checks and a pull request's first 10 closing issues are asked for, so more show no glyph, a check or a link — close it when a project has that many.
    private static final String QUERY = """
            query($owner: String!, $repo: String!) {
              repository(owner: $owner, name: $repo) {
                pullRequests(states: OPEN, first: 100, orderBy: {field: CREATED_AT, direction: DESC}) {
                  nodes {
                    number title headRefName isDraft isCrossRepository createdAt
                    baseRepository { nameWithOwner }
                    comments { totalCount }
                    reviewThreads(first: 100) { nodes { comments { totalCount } } }
                    reviews(states: PENDING, first: 1) { nodes { comments { totalCount } } }
                    closingIssuesReferences(first: 10) { nodes { number repository { nameWithOwner } } }
                    commits(last: 1) {
                      nodes { commit { statusCheckRollup { contexts(first: 100) { nodes {
                        __typename
                        ... on CheckRun { name status conclusion }
                        ... on StatusContext { context state }
                      } } } } }
                    }
                  }
                }
              }
            }
            """;
    private static final String PULL_REQUESTS = ".data.repository.pullRequests.nodes";
    private static final String OWNER = "owner";
    private static final String THIS_OWNER = "{owner}";
    private static final String REPO = "repo";
    private static final String THIS_REPO = "{repo}";
    private static final List<String> ARGUMENTS = new GraphqlCall()
            .typed(OWNER, THIS_OWNER)
            .typed(REPO, THIS_REPO)
            .query(QUERY)
            .picking(PULL_REQUESTS)
            .arguments();

    private final GhCommand gh;

    public GhPullRequestsRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public List<PullRequest> openPullRequests(Project project) {
        return PullRequestRecords.pullRequests(gh.output(project, ARGUMENTS), GhCommand.sourceOf(ARGUMENTS));
    }
}
