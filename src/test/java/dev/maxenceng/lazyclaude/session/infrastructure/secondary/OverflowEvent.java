package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.nio.file.WatchEvent;

final class OverflowEvent implements WatchEvent<Object> {

    @Override
    public Kind<Object> kind() {
        return OVERFLOW;
    }

    @Override
    public int count() {
        return 1;
    }

    @Override
    public Object context() {
        return null;
    }
}
