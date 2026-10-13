package dev.maxenceng.lazyclaude.review.domain;

/** Tells the domain when to read the open pull requests again. */
public interface PullRequestWatchPort {

    /**
     * Runs {@code refresh} on a thread of the watcher's own: at once, then on a fixed interval, for as long as the process lives. A
     * run never starts before the one before it has returned.
     */
    void watch(Runnable refresh);
}
