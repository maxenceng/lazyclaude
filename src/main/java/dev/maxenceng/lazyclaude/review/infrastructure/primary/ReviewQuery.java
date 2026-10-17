package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;

/** What the shell asks of {@code review}: the open and the merged pull requests of a project as views, and a view again after each fetch. It only reads. */
public final class ReviewQuery {

    private final ReviewApplicationService review;

    public ReviewQuery(ReviewApplicationService review) {
        this.review = review;
    }

    /** What stands until the first fetch answers. */
    public PullRequestsView checking() {
        return PullRequestsView.from(review.checking());
    }

    /** Fetches again now rather than at the next interval; a fetch already under way answers it. */
    public void refresh() {
        review.runNow();
    }

    /**
     * Hands {@code listener} the open pull requests and {@code mergedListener} the merged ones after each fetch, the first at once,
     * on a thread of the watcher's own, never a failure.
     */
    public void subscribe(
            ReviewPayload payload, Consumer<PullRequestsView> listener, Consumer<MergedPullRequestsView> mergedListener) {
        review.subscribe(
                payload.toDomain(),
                read -> listener.accept(PullRequestsView.from(read)),
                merged -> mergedListener.accept(MergedPullRequestsView.from(merged)));
    }
}
