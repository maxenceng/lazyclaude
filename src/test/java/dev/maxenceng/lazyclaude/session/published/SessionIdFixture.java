package dev.maxenceng.lazyclaude.session.published;

public final class SessionIdFixture {

    private SessionIdFixture() {}

    public static SessionId sessionId() {
        return new SessionId("70c67de2-4563-47c2-b946-eb5198915ba2");
    }

    public static SessionId secondSessionId() {
        return new SessionId("a3d7d4b5-4cdb-479e-a82e-622d2542a6b8");
    }

    public static SessionId thirdSessionId() {
        return new SessionId("b23a3278-4c1b-4b68-becf-175c9a0dea94");
    }

    public static SessionId fourthSessionId() {
        return new SessionId("c9e1f2a0-77d3-4d1b-8f55-0a4b9c6d1e32");
    }
}
