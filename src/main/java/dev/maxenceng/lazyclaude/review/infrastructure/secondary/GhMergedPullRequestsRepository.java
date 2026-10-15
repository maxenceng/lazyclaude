package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Asks {@code gh pr list} for the merged pull requests of the repository the project's directory belongs to, in one call. A pull
 * request from a fork is left out: its head branch carries no owner, so it could name a branch of this repository that was never merged.
 */
public final class GhMergedPullRequestsRepository implements MergedPullRequestPort {

    // deferred: only the 100 most recent merged pull requests are asked for, so an older merge is not seen — close it when a ticket branch whose merge is that old shows a stale status.
    private static final List<String> ARGUMENTS = List.of(
            "pr", "list", "--state", "merged", "--limit", "100", "--json", "headRefName,headRefOid,number,isCrossRepository");

    private final GhCommand gh;

    public GhMergedPullRequestsRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public List<MergedPullRequest> mergedPullRequests(Project project) {
        return MergedPullRequestRecords.pullRequests(gh.output(project, ARGUMENTS), GhCommand.SOURCE);
    }
}
