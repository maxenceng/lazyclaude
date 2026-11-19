package dev.maxenceng.lazyclaude.session.domain;

public final class HostFallbackFixture {

    private HostFallbackFixture() {}

    public static HostFallback unheeded() {
        return cause -> {};
    }
}
