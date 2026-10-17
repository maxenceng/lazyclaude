package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.review.published.HeadCommit;

/** One merged pull request as the shell reads it: the branch it was opened from, and the commit that branch was merged at. */
public record MergedHeadView(HeadBranch branch, HeadCommit commit) {

    public MergedHeadView {
        Assert.notNull("head branch", branch);
        Assert.notNull("head commit", commit);
    }

    static MergedHeadView from(MergedPullRequest pullRequest) {
        return builder().branch(pullRequest.head()).commit(pullRequest.headCommit()).build();
    }

    public static MergedHeadViewBuilder builder() {
        return new MergedHeadViewBuilder();
    }

    public static final class MergedHeadViewBuilder {
        private HeadBranch branch;
        private HeadCommit commit;

        private MergedHeadViewBuilder() {}

        public MergedHeadViewBuilder branch(HeadBranch branch) {
            this.branch = branch;
            return this;
        }

        public MergedHeadViewBuilder commit(HeadCommit commit) {
            this.commit = commit;
            return this;
        }

        public MergedHeadView build() {
            return new MergedHeadView(branch, commit);
        }
    }
}
