package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Text read from a file's bytes: strict UTF-8, without a leading byte order mark. */
public final class Utf8Text {

    private static final String BYTE_ORDER_MARK = "﻿";

    private Utf8Text() {}

    /** The text the bytes spell, or nothing when they are not valid UTF-8. */
    public static Optional<String> decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return Optional.of(text.startsWith(BYTE_ORDER_MARK) ? text.substring(BYTE_ORDER_MARK.length()) : text);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }
}
