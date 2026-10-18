package dev.maxenceng.lazyclaude.review.domain;

/** What a read of a pull request's threads came to: not answered yet, the threads, or the reason they could not be read. */
public sealed interface ThreadsReading permits LoadingThreads, ReadThreads, UnavailableThreads {}
