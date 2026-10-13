package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;

/** What the shell asks of {@code review}: the open pull requests of a project as a view, and a view again after each fetch. It only reads. */
public final class ReviewQuery {

    private final ReviewApplicationService review;

    public ReviewQuery(ReviewApplicationService review) {
        this.review = review;
    }

    /** What stands until the first fetch answers. */
    public PullRequestsView checking() {
        return PullRequestsView.from(review.checking());
    }

    /** Hands {@code listener} a view after each fetch, the first at once, on a thread of the watcher's own, never a failure. */
    public void subscribe(ReviewPayload payload, Consumer<PullRequestsView> listener) {
        review.subscribe(payload.toDomain(), read -> listener.accept(PullRequestsView.from(read)));
    }
}
