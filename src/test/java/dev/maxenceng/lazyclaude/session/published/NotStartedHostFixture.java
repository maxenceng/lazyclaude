package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;

public final class NotStartedHostFixture {

    private NotStartedHostFixture() {}

    public static NotStartedHost notStartedHost() {
        return new NotStartedHost(reason());
    }

    public static NotStartedHost notStartedBecause(String why) {
        return new NotStartedHost(new Reason(why));
    }
}
