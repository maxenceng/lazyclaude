package dev.maxenceng.lazyclaude.review.application;

import dev.maxenceng.lazyclaude.review.domain.DiffReading;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffManager;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.ThreadPort;
import dev.maxenceng.lazyclaude.review.domain.ThreadsInDiff;
import dev.maxenceng.lazyclaude.review.domain.ThreadsReading;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a pull request's diff and its review threads, each when asked. */
public final class PullRequestDiffApplicationService {

    private final PullRequestDiffManager manager;

    public PullRequestDiffApplicationService(PullRequestDiffPort diffs, ThreadPort threads) {
        this.manager = new PullRequestDiffManager(diffs, threads);
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

    public ThreadsInDiff threadsInDiff(DiffReading diff, ThreadsReading threads) {
        return manager.threadsInDiff(diff, threads);
    }

    public ThreadsReading threads(Project project, PullRequestNumber number) {
        return manager.threads(project, number);
    }
}
