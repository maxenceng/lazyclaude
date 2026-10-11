package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;

/** What the composition root tells the shared kernel: a thread stopped, so the screen says so instead of the JVM printing over it. */
public final class WarningCommand {

    private final WarningApplicationService warnings;

    public WarningCommand(WarningApplicationService warnings) {
        this.warnings = warnings;
    }

    public void threadStopped(StoppedThreadPayload payload) {
        warnings.threadStopped(payload.thread(), payload.failure().toString());
    }
}
