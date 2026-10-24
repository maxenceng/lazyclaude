package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.review.published.NoDiffLine;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.review.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads a pull request's diff and threads when asked. It owns the rule that a read which fails is answered as unavailable, never
 * swallowed and never replaced by the last good read: what is shown is always the latest answer.
 */
public record PullRequestDiffManager(PullRequestDiffPort diffs, ThreadPort threads) {

    /** What stands until the diff's read answers. */
    public DiffReading loadingDiff() {
        return new LoadingDiff();
    }

    /** What stands until the threads' read answers. */
    public ThreadsReading loadingThreads() {
        return new LoadingThreads();
    }

    public DiffReading diff(Project project, PullRequestNumber number) {
        try {
            return new ReadDiff(diffs.diff(project, number));
        } catch (PullRequestDiffTooLargeException tooLarge) {
            return UnavailableDiff.builder().reason(new Reason(tooLarge.getMessage())).tooLarge(true).build();
        } catch (PullRequestsUnavailableException noAnswer) {
            return UnavailableDiff.builder().reason(new Reason(noAnswer.getMessage())).tooLarge(false).build();
        }
    }

    /**
     * The threads with the diff lines they show under, from whatever each read last answered: none until the threads are read, and every
     * one on no line until the diff is.
     */
    public ThreadsInDiff threadsInDiff(DiffReading diff, ThreadsReading threads) {
        if (!(threads instanceof ReadThreads read)) {
            return new ThreadsInDiff(List.of());
        }
        if (diff instanceof ReadDiff(PullRequestDiff placed)) {
            return placed.place(read);
        }
        return new ThreadsInDiff(read.byFileAndLine().stream().map(thread -> ThreadInDiff.builder().thread(thread).line(new NoDiffLine()).build()).toList());
    }

    public ThreadsReading threads(Project project, PullRequestNumber number) {
        try {
            return new ReadThreads(threads.threads(project, number));
        } catch (PullRequestsUnavailableException noAnswer) {
            return new UnavailableThreads(new Reason(noAnswer.getMessage()));
        }
    }
}
