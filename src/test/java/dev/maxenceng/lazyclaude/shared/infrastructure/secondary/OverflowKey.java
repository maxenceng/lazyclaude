package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.List;

final class OverflowKey implements WatchKey {

    private final Path directory;

    OverflowKey(Path directory) {
        this.directory = directory;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
        return List.of(new WatchEvent<Object>() {
            @Override
            public Kind<Object> kind() {
                return StandardWatchEventKinds.OVERFLOW;
            }

            @Override
            public int count() {
                return 1;
            }

            @Override
            public Object context() {
                return null;
            }
        });
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
