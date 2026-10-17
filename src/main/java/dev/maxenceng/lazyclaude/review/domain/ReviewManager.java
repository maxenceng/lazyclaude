package dev.maxenceng.lazyclaude.review.domain;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads a project's open and merged pull requests whenever the watch says to. It owns the rule that a run which fails is delivered as
 * unavailable, never swallowed and never replaced by the last good run: what is shown is always the latest answer.
 */
public record ReviewManager(
        OpenPullRequestPort pullRequests, MergedPullRequestPort mergedPullRequests, PullRequestWatchPort watch) {

    /** What stands until the first run answers. */
    public PullRequests checking() {
        return new CheckingPullRequests();
    }

    /**
     * Hands {@code listener} the open pull requests and {@code mergedListener} the merged ones after each run of the watch, on the
     * watch's thread. Both are read in the one run, so the two fetches share the timer and never overlap.
     */
    public void watch(Project project, Consumer<PullRequests> listener, Consumer<MergedPullRequests> mergedListener) {
        watch.watch(() -> {
            listener.accept(read(project));
            mergedListener.accept(readMerged(project));
        });
    }

    /** Runs the watch's fetch now, for a reader who asked rather than waited; a fetch already under way answers it. */
    public void runNow() {
        watch.runNow();
    }

    private PullRequests read(Project project) {
        try {
            return new OpenPullRequests(pullRequests.openPullRequests(project));
        } catch (PullRequestsUnavailableException noAnswer) {
            return new UnavailablePullRequests(new Reason(noAnswer.getMessage()));
        }
    }

    private MergedPullRequests readMerged(Project project) {
        try {
            return new KnownMergedPullRequests(mergedPullRequests.mergedPullRequests(project));
        } catch (PullRequestsUnavailableException noAnswer) {
            return new UnavailableMergedPullRequests(new Reason(noAnswer.getMessage()));
        }
    }
}
