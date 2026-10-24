package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PullRequestPayload;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/**
 * Your pending review of the selected ticket's pull request, as the ticket detail shows it: read off the render thread when the
 * selection lands on the ticket and again once marked stale, a read that lands after a later one was begun dropped.
 */
final class PendingDetail {

    private final ReviewQuery reviews;
    private final Path root;
    private final Executor background;
    private final Consumer<Runnable> onRenderThread;
    private Optional<ShownPending> shown = Optional.empty();
    private boolean stale;
    private long reads;

    PendingDetail(ReviewQuery reviews, Path root, Executor background, Consumer<Runnable> onRenderThread) {
        this.reviews = reviews;
        this.root = root;
        this.background = background;
        this.onRenderThread = onRenderThread;
    }

    /** The next {@link #sync} reads again, as on {@code R} and when a review is left. */
    void markStale() {
        stale = true;
    }

    /** What was last read for {@code ticket}, if the last read was for it. */
    Optional<PendingReviewView> of(TicketView ticket) {
        return shown.filter(read -> read.ticket().equals(idOf(ticket))).map(ShownPending::view);
    }

    /** Reads {@code ticket}'s pending review on its pull request {@code number}, unless one already read for it still stands. */
    void sync(TicketView ticket, PullRequestNumber number) {
        if (of(ticket).isEmpty() || stale) {
            stale = false;
            read(ticket, number);
        }
    }

    private void read(TicketView ticket, PullRequestNumber number) {
        long read = ++reads;
        shown = Optional.of(new ShownPending(idOf(ticket), reviews.loadingPending()));
        PullRequestPayload payload = new PullRequestPayload(root, number);
        CompletableFuture.supplyAsync(() -> reviews.pendingOf(payload), background)
                .exceptionally(ReviewReads::unreadablePending)
                .thenAccept(answer -> onRenderThread.accept(() -> {
                    if (read == reads) {
                        shown = Optional.of(new ShownPending(idOf(ticket), answer));
                    }
                }));
    }

    private static LinkedTicketId idOf(TicketView ticket) {
        return new LinkedTicketId(ticket.id().value());
    }
}
