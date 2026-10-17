package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.KnownMergedPullRequests;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequests;
import dev.maxenceng.lazyclaude.review.domain.UnavailableMergedPullRequests;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.MergedHead;
import dev.maxenceng.lazyclaude.review.published.NoMergedHead;

/**
 * The merged pull requests as the shell reads them: their head branches and the commits those were merged at. Until the fetch answers, and while GitHub cannot be read, there
 * are none: the shell has nothing to do differently in either case, and nothing to say about it — it starts on none itself.
 */
public record MergedPullRequestsView(List<MergedHeadView> heads) {

    public MergedPullRequestsView {
        Assert.notNull("merged heads", heads);
    }

    /** The names of the head branches, which is how a ticket's branch is found among them. */
    public Set<String> headBranches() {
        return heads.stream().map(head -> head.branch().value()).collect(Collectors.toUnmodifiableSet());
    }

    /** The commit {@code branch}'s merged pull request was merged at, or none when no merged pull request has it as its head. */
    public MergedHead mergedHeadOf(HeadBranch branch) {
        return heads.stream()
                .filter(head -> head.branch().equals(branch))
                .<MergedHead>map(MergedHeadView::commit)
                .findFirst()
                .orElseGet(NoMergedHead::new);
    }

    // deferred: the unavailable reason is dropped here, so no reader can tell an unknown merged set from an empty one — close it when the verb picker needs to say the PR state is unknown rather than preselect respond.
    static MergedPullRequestsView from(MergedPullRequests merged) {
        return new MergedPullRequestsView(
                switch (merged) {
                    case KnownMergedPullRequests known ->
                        known.pullRequests().stream().map(MergedHeadView::from).toList();
                    case UnavailableMergedPullRequests _ -> List.of();
                });
    }
}
