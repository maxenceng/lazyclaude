package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Consumer;

import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.PtyListener;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** Hands each read of a hosted pty to a frontend that emulates it itself, and nothing else the pty's listeners are told. */
final class PtyOutputRelay implements PtyListener {

    private final Consumer<PtyOutputView> listener;

    PtyOutputRelay(Consumer<PtyOutputView> listener) {
        this.listener = listener;
    }

    @Override
    public void launching(SessionId id, PaneSize size) {}

    @Override
    public void output(SessionId id, PtyBytes bytes) {
        listener.accept(PtyOutputView.builder().session(id).data(bytes).build());
    }

    @Override
    public void ended(SessionId id) {}
}
