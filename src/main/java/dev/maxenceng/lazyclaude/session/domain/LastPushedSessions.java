package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The sessions as the watcher last pushed them, before the hosted ones are laid over, so a question asked on the render
 * thread is answered without reading the registry there. Empty until the first push.
 */
public final class LastPushedSessions {

    private final AtomicReference<Optional<Sessions>> last = new AtomicReference<>(Optional.empty());

    public void remember(Sessions pushed) {
        last.set(Optional.of(pushed));
    }

    public Optional<Sessions> last() {
        return last.get();
    }
}
