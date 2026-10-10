package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a session is doing, in lazyclaude's words: working, waiting for your next prompt, or blocked on you. */
public sealed interface SessionState permits Busy, YourTurn, WaitingForPermission, WaitingForInput {

    /** A waiting session blocked on a permission prompt asks for permission; any other reason asks for input. */
    static SessionState of(ReportedStatus status, WaitingFor waitingFor) {
        Assert.notNull("status", status);
        Assert.notNull("waiting for", waitingFor);
        return switch (status) {
            case BUSY, SHELL -> new Busy();
            case IDLE -> new YourTurn();
            case WAITING -> waiting(waitingFor);
        };
    }

    private static SessionState waiting(WaitingFor waitingFor) {
        return switch (waitingFor) {
            case WaitingReason reason when reason.isPermissionPrompt() -> new WaitingForPermission();
            default -> new WaitingForInput(waitingFor);
        };
    }
}
