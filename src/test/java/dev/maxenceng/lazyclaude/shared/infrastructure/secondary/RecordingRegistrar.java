package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingRegistrar implements DirectoryRegistrar {

    private final List<Path> registered = new CopyOnWriteArrayList<>();
    private final List<Path> cancelled = new CopyOnWriteArrayList<>();
    private final int failingCall;
    private final boolean real;

    private RecordingRegistrar(int failingCall, boolean real) {
        this.failingCall = failingCall;
        this.real = real;
    }

    static RecordingRegistrar neverFailing() {
        return new RecordingRegistrar(0, false);
    }

    static RecordingRegistrar failingOnCall(int call) {
        return new RecordingRegistrar(call, true);
    }

    static RecordingRegistrar recordingOnlyFailingOnCall(int call) {
        return new RecordingRegistrar(call, false);
    }

    List<Path> registered() {
        return registered;
    }

    List<Path> cancelled() {
        return cancelled;
    }

    @Override
    public WatchKey register(WatchService service, Path directory) throws IOException {
        registered.add(directory);
        if (registered.size() == failingCall) {
            throw new IOException("User limit of inotify watches reached");
        }
        return real
                ? directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                : new IdleKey(directory, cancelled::add);
    }
}
