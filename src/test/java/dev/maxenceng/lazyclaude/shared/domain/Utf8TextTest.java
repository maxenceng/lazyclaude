package dev.maxenceng.lazyclaude.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class Utf8TextTest {

    @Test
    void shouldDecodeUtf8() {
        assertThat(Utf8Text.decode("café".getBytes(StandardCharsets.UTF_8))).isEqualTo(Optional.of("café"));
    }

    @Test
    void shouldDropALeadingByteOrderMark() {
        assertThat(Utf8Text.decode("﻿# Title".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(Optional.of("# Title"));
    }

    @Test
    void shouldRefuseBytesThatAreNotUtf8() {
        assertThat(Utf8Text.decode("café".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
    }

    @Test
    void shouldDecodeNothingAsEmptyText() {
        assertThat(Utf8Text.decode(new byte[0])).isEqualTo(Optional.of(""));
    }
}
