package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.charset.StandardCharsets;

public final class SentBytesFixture {

    private SentBytesFixture() {}

    public static String sent(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    public static String sentAsUtf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
