package dev.maxenceng.lazyclaude.session.published;

public final class WaitingForFixture {

    private WaitingForFixture() {}

    public static WaitingReason permissionPrompt() {
        return new WaitingReason("permission prompt");
    }

    public static WaitingReason inputNeeded() {
        return new WaitingReason("input needed");
    }
}
