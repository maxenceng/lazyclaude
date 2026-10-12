package dev.maxenceng.lazyclaude.session.published;

import java.util.UUID;

public final class HostedSessionIdFixture {

    private HostedSessionIdFixture() {}

    public static HostedSessionId hostedSessionId() {
        return new HostedSessionId(UUID.fromString("0b9f2a8e-5d34-4c1e-9a77-3f1c2d4e6a10"));
    }

    public static HostedSessionId secondHostedSessionId() {
        return new HostedSessionId(UUID.fromString("7c1d9e02-8b45-4f6a-b3d8-91a0c5e7f2b4"));
    }
}
