package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.List;
import java.util.function.Consumer;

final class IdleKey implements WatchKey {

    private final Path directory;
    private final Consumer<Path> onCancel;

    IdleKey(Path directory, Consumer<Path> onCancel) {
        this.directory = directory;
        this.onCancel = onCancel;
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
    public void cancel() {
        onCancel.accept(directory);
    }

    @Override
    public Watchable watchable() {
        return directory;
    }
}
