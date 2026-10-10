package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;

public final class SessionStateFixture {

    private SessionStateFixture() {}

    public static SessionState waitingForInput() {
        return new WaitingForInput(inputNeeded());
    }
}
