package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Asks {@code gh pr list} for the open pull requests of the repository the project's directory belongs to, with their checks, in one
 * call. A pull request from a fork is left out: its head branch carries no owner, so it could shadow this repository's own. Any way {@code gh} fails to answer, a missing binary, a login it lacks or a run it was killed in, is the one unavailable
 * reason, in the words {@code gh} used where it used any.
 */
public final class GhPullRequestsRepository implements OpenPullRequestPort {

    private static final List<String> ARGUMENTS =
            List.of("pr", "list", "--state", "open", "--json", "number,title,headRefName,isDraft,isCrossRepository,statusCheckRollup");

    private final GhCommand gh;

    public GhPullRequestsRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public List<PullRequest> openPullRequests(Project project) {
        return PullRequestRecords.pullRequests(gh.output(project, ARGUMENTS), GhCommand.SOURCE);
    }
}
