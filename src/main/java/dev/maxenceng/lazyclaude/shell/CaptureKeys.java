package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.CapturedTicketView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.RefusedTicketCaptureView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCapturePayload;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureView;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.session.published.FixedChord;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

/**
 * The keys of the popup a draft ticket is captured in (ADR 0054): {@code enter} creates the issue off the render thread, {@code esc}
 * drops the popup. A refused create keeps the popup with the typed title and says why; nothing retries until {@code enter} is pressed again.
 */
final class CaptureKeys {

    private final TicketCaptureCommand capturing;
    private final Path root;
    private final Executor background;
    private final Consumer<Runnable> onRenderThread;
    private final Consumer<TicketId> captured;
    private Optional<CaptureBox> box = Optional.empty();

    /** {@code captured} is told the number GitHub gave the new ticket, on the render thread, once the popup has closed. */
    CaptureKeys(
            TicketCaptureCommand capturing,
            Path root,
            Executor background,
            Consumer<Runnable> onRenderThread,
            Consumer<TicketId> captured) {
        this.capturing = capturing;
        this.root = root;
        this.background = background;
        this.onRenderThread = onRenderThread;
        this.captured = captured;
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
        if (open.creating()) {
            return true;
        }
        if (key.isKey(KeyCode.ESCAPE)) {
            box = Optional.empty();
        } else if (key.isKey(KeyCode.ENTER)) {
            create(open);
        } else {
            open.edit(key);
        }
        return true;
    }

    /** Asks GitHub off the render thread; the answer closes the popup, or keeps it with the reason it was refused. */
    private void create(CaptureBox open) {
        open.createBegun();
        TicketCapturePayload payload = new TicketCapturePayload(root, open.content());
        CompletableFuture.supplyAsync(() -> capturing.capture(payload), background)
                .exceptionally(CaptureReads::refused)
                .thenAccept(answer -> onRenderThread.accept(() -> answered(open, answer)));
    }

    private void answered(CaptureBox open, TicketCaptureView answer) {
        switch (answer) {
            case CapturedTicketView taken -> {
                box = Optional.empty();
                captured.accept(taken.id());
            }
            case RefusedTicketCaptureView refused -> open.createRefused(refused.reason());
        }
    }
}
