package dev.maxenceng.lazyclaude.session.domain;

import java.nio.charset.StandardCharsets;

public final class PtyBytesFixture {

    private PtyBytesFixture() {}

    public static PtyBytes ptyBytesOf(String text) {
        return new PtyBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    public static PtyBytes typedBytes() {
        return ptyBytesOf("a");
    }

    public static String utf8Of(PtyBytes bytes) {
        return new String(bytes.value(), StandardCharsets.UTF_8);
    }
}
