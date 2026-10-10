package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingRegistrar implements DirectoryRegistrar {

    private final List<Path> registered = new CopyOnWriteArrayList<>();
    private final int failingCall;

    RecordingRegistrar(int failingCall) {
        this.failingCall = failingCall;
    }

    static RecordingRegistrar neverFailing() {
        return new RecordingRegistrar(0);
    }

    static RecordingRegistrar failingOnCall(int call) {
        return new RecordingRegistrar(call);
    }

    List<Path> registered() {
        return registered;
    }

    @Override
    public void register(WatchService service, Path directory) throws IOException {
        registered.add(directory);
        if (registered.size() == failingCall) {
            throw new IOException("User limit of inotify watches reached");
        }
    }
}
