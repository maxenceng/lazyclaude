package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.List;

final class IdleKey implements WatchKey {

    private final Path directory;

    IdleKey(Path directory) {
        this.directory = directory;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        return List.of();
    }

    @Override
    public boolean reset() {
        return true;
    }

    @Override
    public void cancel() {}

    @Override
    public Watchable watchable() {
        return directory;
    }
}
