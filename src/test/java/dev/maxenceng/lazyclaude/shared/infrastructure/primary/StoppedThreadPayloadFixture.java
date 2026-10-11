package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

public final class StoppedThreadPayloadFixture {

    private StoppedThreadPayloadFixture() {}

    public static StoppedThreadPayload stoppedDocsWatch() {
        return new StoppedThreadPayload("docs-watch", new IllegalStateException("gone"));
    }
}
