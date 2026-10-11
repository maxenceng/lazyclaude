package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

/** What {@link WarningCommand} is told when a thread ends on a failure nobody caught: the thread's name and the failure. */
public record StoppedThreadPayload(String thread, Throwable failure) {}
