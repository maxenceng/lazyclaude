package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads the issues of the GitHub repository the project's directory belongs to with {@code gh api graphql}: the open ones and the closed
 * ones, every page of one query, each with the branch GitHub links to it and the pull request that closed it (ADR 0118, ADR 0120). Nothing is kept between reads. A run that ends non-zero is the reason the board cannot be read, in the words
 * {@code gh} used where it used any.
 */
public final class IssuesRepository implements BacklogPort {

    private static final String QUERY = """
            query($owner: String!, $name: String!, $endCursor: String) { \
            repository(owner: $owner, name: $name) { \
            issues(first: 100, after: $endCursor, states: [OPEN, CLOSED], orderBy: {field: CREATED_AT, direction: DESC}) { \
            nodes { number title body state stateReason closedAt \
            labels(first: 100) { nodes { name } } linkedBranches(first: 1) { nodes { ref { name } } } \
            timelineItems(itemTypes: [CLOSED_EVENT], last: 1) { nodes { ... on ClosedEvent { closer { ... on PullRequest { headRefName merged } } } } } } \
            pageInfo { hasNextPage endCursor } } } }""";
    static final List<String> ARGUMENTS = List.of(
            "api", "graphql", "--paginate", "--slurp", "-F", "owner={owner}", "-F", "name={repo}", "-f", "query=" + QUERY);

    private final GhCommand gh;

    public IssuesRepository() {
        this(new GhCommand());
    }

    IssuesRepository(GhCommand gh) {
        this.gh = gh;
    }

    @Override
    public List<BacklogEntry> entries(Project project) {
        GhResult listed = gh.run(project, ARGUMENTS);
        if (!listed.succeeded()) {
            throw GhFailure.of(listed);
        }
        return IssueRecords.entries(listed.output());
    }
}
