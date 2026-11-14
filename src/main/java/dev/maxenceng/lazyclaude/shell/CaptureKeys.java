package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NewTicketHostedPayload;
import dev.maxenceng.lazyclaude.session.published.FixedChord;
import dev.maxenceng.lazyclaude.session.published.HostStart;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.StartedHost;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * The keys of the popup a new ticket is described in (ADR 0110): {@code enter} starts the session that creates it off the render
 * thread, {@code esc} drops the popup. A blank description is refused here and starts nothing. A session that did not start keeps
 * the popup with the typed description and says why; nothing retries until {@code enter} is pressed again.
 */
final class CaptureKeys {

    private final HostedSessionCommand hosting;
    private final Path root;
    private final Supplier<PaneSize> paneSize;
    private final Executor background;
    private final Consumer<Runnable> onRenderThread;
    private final Consumer<StartedHost> started;
    private Optional<CaptureBox> box = Optional.empty();

    /** {@code started} is told the session that was started, on the render thread, once the popup has closed. */
    CaptureKeys(
            HostedSessionCommand hosting,
            Path root,
            Supplier<PaneSize> paneSize,
            Executor background,
            Consumer<Runnable> onRenderThread,
            Consumer<StartedHost> started) {
        this.hosting = hosting;
        this.root = root;
        this.paneSize = paneSize;
        this.background = background;
        this.onRenderThread = onRenderThread;
        this.started = started;
    }

    Optional<CaptureBox> box() {
        return box;
    }

    void open() {
        box = Optional.of(new CaptureBox());
    }

    /** The popup takes every key; only {@code ctrl+c} is left to quit. */
    Optional<Boolean> press(KeyEvent key) {
        return box.filter(_ -> !EmulatorInputs.isFixed(key, FixedChord.QUIT)).map(open -> pressIn(open, key));
    }

    private boolean pressIn(CaptureBox open, KeyEvent key) {
        if (open.starting()) {
            return true;
        }
        if (key.isKey(KeyCode.ESCAPE)) {
            box = Optional.empty();
        } else if (key.isKey(KeyCode.ENTER)) {
            start(open);
        } else {
            open.edit(key);
        }
        return true;
    }

    private void start(CaptureBox open) {
        if (open.content().isBlank()) {
            open.startRefused(new Reason(CaptureText.BLANK));
            return;
        }
        open.startBegun();
        NewTicketHostedPayload payload = new NewTicketHostedPayload(root, open.content(), paneSize.get());
        CompletableFuture.supplyAsync(() -> hosting.startNewTicket(payload), background)
                .whenComplete((answer, failure) -> onRenderThread.accept(() -> {
                    if (failure == null) {
                        answered(open, answer);
                    } else {
                        open.startRefused(failedBecause(failure));
                    }
                }));
    }

    private static Reason failedBecause(Throwable failure) {
        Throwable cause = failure instanceof CompletionException wrapped && wrapped.getCause() != null ? wrapped.getCause() : failure;
        return new Reason(Optional.ofNullable(cause.getMessage())
                .filter(message -> !message.isBlank())
                .orElseGet(() -> cause.getClass().getSimpleName()));
    }

    private void answered(CaptureBox open, HostStart answer) {
        switch (answer) {
            case StartedHost taken -> {
                box = Optional.empty();
                started.accept(taken);
            }
            case NotStartedHost refused -> open.startRefused(refused.reason());
        }
    }
}
