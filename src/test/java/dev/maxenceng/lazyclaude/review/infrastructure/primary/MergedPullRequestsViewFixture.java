package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.review.published.HeadBranch;

public final class MergedPullRequestsViewFixture {

    private MergedPullRequestsViewFixture() {}

    public static MergedPullRequestsView mergedPullRequestsViewOf(HeadBranch... heads) {
        return new MergedPullRequestsView(List.of(heads));
    }

    public static MergedPullRequestsView noMergedPullRequestsView() {
        return new MergedPullRequestsView(List.of());
    }

    public static MergedPullRequestsView mergedPullRequestsViewOn(String... heads) {
        return new MergedPullRequestsView(Arrays.stream(heads).map(HeadBranch::new).toList());
    }
}
