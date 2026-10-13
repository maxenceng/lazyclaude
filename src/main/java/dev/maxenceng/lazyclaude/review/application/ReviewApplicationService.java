package dev.maxenceng.lazyclaude.review.application;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.domain.OpenPullRequestPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestWatchPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequests;
import dev.maxenceng.lazyclaude.review.domain.ReviewManager;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads a project's open pull requests, and keeps a listener handed them again as the watch runs. */
public final class ReviewApplicationService {

    private final ReviewManager manager;

    public ReviewApplicationService(OpenPullRequestPort pullRequests, PullRequestWatchPort watch) {
        this.manager = new ReviewManager(pullRequests, watch);
    }

    public PullRequests checking() {
        return manager.checking();
    }

    public void subscribe(Project project, Consumer<PullRequests> listener) {
        manager.watch(project, listener);
    }
}
