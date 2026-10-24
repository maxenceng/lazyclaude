package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.WatchService;

/** The watcher waits on {@code service}. */
public record Watching(WatchService service) implements WatchServiceState {}
