package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingRegistrar implements DirectoryRegistrar {

    private static final DirectoryRegistrar RECORD_ONLY = (service, directory) -> {};
    private static final DirectoryRegistrar REAL = (service, directory) ->
            directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

    private final List<Path> registered = new CopyOnWriteArrayList<>();
    private final int failingCall;
    private final DirectoryRegistrar delegate;

    private RecordingRegistrar(int failingCall, DirectoryRegistrar delegate) {
        this.failingCall = failingCall;
        this.delegate = delegate;
    }

    static RecordingRegistrar neverFailing() {
        return new RecordingRegistrar(0, RECORD_ONLY);
    }

    static RecordingRegistrar failingOnCall(int call) {
        return new RecordingRegistrar(call, REAL);
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
        delegate.register(service, directory);
    }
}
