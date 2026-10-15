package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.KnownMergedPullRequests;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequests;
import dev.maxenceng.lazyclaude.review.domain.UnavailableMergedPullRequests;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;

/**
 * The merged pull requests as the shell reads them: their head branches. Until the fetch answers, and while GitHub cannot be read, there
 * are none: the shell has nothing to do differently in either case, and nothing to say about it — it starts on none itself.
 */
public record MergedPullRequestsView(List<HeadBranch> heads) {

    public MergedPullRequestsView {
        Assert.notNull("head branches", heads);
    }

    /** The names of the head branches, which is how a ticket's branch is found among them. */
    public Set<String> headBranches() {
        return heads.stream().map(HeadBranch::value).collect(Collectors.toUnmodifiableSet());
    }

    static MergedPullRequestsView from(MergedPullRequests merged) {
        return new MergedPullRequestsView(
                switch (merged) {
                    case KnownMergedPullRequests known ->
                        known.pullRequests().stream().map(MergedPullRequest::head).toList();
                    case UnavailableMergedPullRequests _ -> List.of();
                });
    }
}
