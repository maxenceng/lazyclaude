package dev.maxenceng.lazyclaude.review.domain;

import static dev.maxenceng.lazyclaude.review.published.ReasonFixture.reason;

public final class PullRequestDiffManagerFixture {

    private PullRequestDiffManagerFixture() {}

    public static PullRequestDiffTooLargeException diffTooLarge() {
        return new PullRequestDiffTooLargeException(reason().value(), new IllegalStateException());
    }

    public static UnavailableDiff unavailableDiff() {
        return UnavailableDiff.builder().reason(reason()).tooLarge(false).build();
    }

    public static UnavailableDiff tooLargeDiff() {
        return UnavailableDiff.builder().reason(reason()).tooLarge(true).build();
    }

    public static UnavailableDiff unavailableDiffSayingNothingOfItsSize() {
        return UnavailableDiff.builder().reason(reason()).build();
    }

    public static UnavailableThreads unavailableThreads() {
        return new UnavailableThreads(reason());
    }
}
