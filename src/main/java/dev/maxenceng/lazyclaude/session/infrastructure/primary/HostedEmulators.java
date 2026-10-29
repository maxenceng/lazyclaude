package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.domain.PtyListener;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/** The emulator of each hosted session that is running: made when it is about to spawn, fed what it writes, and dropped when it ends. */
public final class HostedEmulators implements PtyListener {

    private final SessionApplicationService sessions;
    private final Map<SessionId, HostedEmulator> running = new ConcurrentHashMap<>();
    private final List<Runnable> screenListeners = new CopyOnWriteArrayList<>();

    private HostedEmulators(SessionApplicationService sessions) {
        this.sessions = sessions;
    }

    /** The emulators of {@code sessions}' hosted sessions, told of each launch, output and end from now on. */
    public static HostedEmulators listeningTo(SessionApplicationService sessions) {
        HostedEmulators emulators = new HostedEmulators(sessions);
        sessions.onPty(emulators);
        return emulators;
    }

    Optional<HostedEmulator> of(SessionId id) {
        return Optional.ofNullable(running.get(id));
    }

    void onScreenChange(Runnable listener) {
        screenListeners.add(listener);
    }

    @Override
    public void launching(SessionId id, PaneSize size) {
        HostedEmulator emulator = new HostedEmulator(id, size, replies -> sessions.write(id, replies));
        Optional.ofNullable(running.put(id, emulator)).ifPresent(HostedEmulator::end);
        emulator.start(() -> screenListeners.forEach(Runnable::run));
    }

    @Override
    public void output(SessionId id, PtyBytes bytes) {
        of(id).ifPresent(emulator -> emulator.feed(bytes));
    }

    @Override
    public void ended(SessionId id) {
        Optional.ofNullable(running.remove(id)).ifPresent(HostedEmulator::end);
    }
}
