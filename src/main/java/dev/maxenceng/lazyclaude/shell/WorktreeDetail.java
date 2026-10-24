package dev.maxenceng.lazyclaude.shell;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * The selected ticket's worktree, as the ticket detail and {@code d} judge it: read off the render thread when another ticket is
 * selected or once marked stale, one read at a time, and kept with the ticket it is about.
 */
final class WorktreeDetail {

    private final SessionQuery screens;
    private final Executor background;
    private final Consumer<Runnable> onRenderThread;
    private final AtomicBoolean reading = new AtomicBoolean();
    private Optional<ShownWorktree> shown = Optional.empty();
    private boolean stale;

    WorktreeDetail(SessionQuery screens, Executor background, Consumer<Runnable> onRenderThread) {
        this.screens = screens;
        this.background = background;
        this.onRenderThread = onRenderThread;
    }

    /** Something the worktree is judged by changed, so the next {@link #sync} reads again. */
    void markStale() {
        stale = true;
    }

    /** The worktree is gone: nothing is shown until it is read again. */
    void forget() {
        shown = Optional.empty();
        stale = true;
    }

    /** What was last read for {@code ticket}, if the last read was for it. */
    Optional<WorktreeView> of(TicketView ticket) {
        return shown.filter(read -> read.ticket().equals(idOf(ticket))).map(ShownWorktree::view);
    }

    /** Reads {@code ticket}'s worktree as {@code payload} asks, unless one already read for it still stands or a read is under way. */
    void sync(TicketView ticket, Supplier<WorktreePayload> payload) {
        if ((of(ticket).isEmpty() || stale) && reading.compareAndSet(false, true)) {
            stale = false;
            read(ticket, payload.get());
        }
    }

    /** A read that fails unexpectedly is shown as unreadable, so the next read is not held back by one that never landed. */
    private void read(TicketView ticket, WorktreePayload payload) {
        CompletableFuture.supplyAsync(() -> screens.worktreeOf(payload), background)
                .exceptionally(WorktreeDetail::unreadable)
                .thenAccept(read -> onRenderThread.accept(() -> {
                    shown = Optional.of(new ShownWorktree(idOf(ticket), read));
                    reading.set(false);
                }));
    }

    private static WorktreeView unreadable(Throwable failed) {
        Throwable cause = failed instanceof CompletionException wrapped && wrapped.getCause() != null ? wrapped.getCause() : failed;
        return new UnreadableWorktreeView(new Reason(Optional.ofNullable(cause.getMessage())
                .filter(message -> !message.isBlank())
                .orElseGet(() -> cause.getClass().getSimpleName())));
    }

    private static LinkedTicketId idOf(TicketView ticket) {
        return new LinkedTicketId(ticket.id().value());
    }
}
