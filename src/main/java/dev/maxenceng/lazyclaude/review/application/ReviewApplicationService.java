package dev.maxenceng.lazyclaude.review.application;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequests;
import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequests;
import dev.maxenceng.lazyclaude.review.domain.ReviewManager;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a project's open and merged pull requests, and keeps listeners handed them again as the watch runs. */
public final class ReviewApplicationService {

    private final ReviewManager manager;

    public ReviewApplicationService(
            OpenPullRequestPort pullRequests, MergedPullRequestPort mergedPullRequests, PullRequestWatchPort watch) {
        this.manager = new ReviewManager(pullRequests, mergedPullRequests, watch);
    }

    public PullRequests checking() {
        return manager.checking();
    }

    public void subscribe(Project project, Consumer<PullRequests> listener, Consumer<MergedPullRequests> mergedListener) {
        manager.watch(project, listener, mergedListener);
    }
}
