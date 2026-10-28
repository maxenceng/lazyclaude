package dev.maxenceng.lazyclaude.session.domain;

import java.util.Arrays;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What crosses a hosted session's pty in one go, as the bytes themselves: output as {@code claude} wrote it, input as the frontend's terminal encoded it. */
public record PtyBytes(byte[] value) {

    public PtyBytes {
        Assert.notNull("pty bytes", value);
        value = value.clone();
    }

    @Override
    public byte[] value() {
        return value.clone();
    }

    public boolean isEmpty() {
        return value.length == 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PtyBytes that && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "PtyBytes[%d]".formatted(value.length);
    }
}
