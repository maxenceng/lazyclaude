package dev.maxenceng.lazyclaude.session.published;

public final class SessionNameFixture {

    private SessionNameFixture() {}

    public static SessionName sessionName() {
        return new SessionName("lazyclaude-b6");
    }

    public static SessionName secondSessionName() {
        return new SessionName("lazyclaude-3f");
    }

    public static SessionName thirdSessionName() {
        return new SessionName("claude-tools-a1");
    }
}
