package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

/** The watcher has no service to wait on, for {@code reason}. */
public record NotWatching(String reason) implements WatchServiceState {}
