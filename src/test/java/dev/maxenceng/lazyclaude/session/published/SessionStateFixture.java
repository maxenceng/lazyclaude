package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;

public final class SessionStateFixture {

    private SessionStateFixture() {}

    public static SessionState waitingForPermission() {
        return new WaitingForPermission(permissionPrompt());
    }

    public static SessionState waitingForInput() {
        return new WaitingForInput(inputNeeded());
    }
}
