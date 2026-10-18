package dev.maxenceng.lazyclaude.review.application;

import dev.maxenceng.lazyclaude.review.domain.DiffReading;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestReviewManager;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;
import dev.maxenceng.lazyclaude.review.domain.ThreadsReading;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a pull request's diff and its review threads, each when asked. */
public final class PullRequestReviewApplicationService {

    private final PullRequestReviewManager manager;

    public PullRequestReviewApplicationService(PullRequestDiffPort diffs, ThreadPort threads) {
        this.manager = new PullRequestReviewManager(diffs, threads);
    }

    public DiffReading loadingDiff() {
        return manager.loadingDiff();
    }

    public ThreadsReading loadingThreads() {
        return manager.loadingThreads();
    }

    public DiffReading diff(Project project, PullRequestNumber number) {
        return manager.diff(project, number);
    }

    public ThreadsReading threads(Project project, PullRequestNumber number) {
        return manager.threads(project, number);
    }
}
