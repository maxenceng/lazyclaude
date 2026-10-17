package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.headBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadBranchFixture.siblingHeadBranch;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.headCommit;
import static dev.maxenceng.lazyclaude.review.published.HeadCommitFixture.secondHeadCommit;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.review.published.HeadBranch;

public final class MergedPullRequestsViewFixture {

    private MergedPullRequestsViewFixture() {}

    public static MergedHeadView mergedHeadView() {
        return MergedHeadView.builder().branch(headBranch()).commit(headCommit()).build();
    }

    public static MergedHeadView siblingMergedHeadView() {
        return MergedHeadView.builder().branch(siblingHeadBranch()).commit(secondHeadCommit()).build();
    }

    public static MergedPullRequestsView mergedPullRequestsViewOf(MergedHeadView... heads) {
        return new MergedPullRequestsView(List.of(heads));
    }

    public static MergedPullRequestsView noMergedPullRequestsView() {
        return new MergedPullRequestsView(List.of());
    }

    public static MergedPullRequestsView mergedPullRequestsViewOn(String... heads) {
        return new MergedPullRequestsView(Arrays.stream(heads)
                .map(head -> MergedHeadView.builder().branch(new HeadBranch(head)).commit(headCommit()).build())
                .toList());
    }
}
