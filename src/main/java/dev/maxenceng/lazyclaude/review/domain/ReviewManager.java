package dev.maxenceng.lazyclaude.review.domain;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads a project's open pull requests whenever the watch says to. It owns the rule that a run which fails is delivered as
 * unavailable, never swallowed and never replaced by the last good run: what is shown is always the latest answer.
 */
public record ReviewManager(OpenPullRequestPort pullRequests, PullRequestWatchPort watch) {

    /** What stands until the first run answers. */
    public PullRequests checking() {
        return new CheckingPullRequests();
    }

    /** Hands {@code listener} the outcome of each run of the watch, on the watch's thread. */
    public void watch(Project project, Consumer<PullRequests> listener) {
        watch.watch(() -> listener.accept(read(project)));
    }

    private PullRequests read(Project project) {
        try {
            return new OpenPullRequests(pullRequests.openPullRequests(project));
        } catch (PullRequestsUnavailableException noAnswer) {
            return new UnavailablePullRequests(new Reason(noAnswer.getMessage()));
        }
    }
}
