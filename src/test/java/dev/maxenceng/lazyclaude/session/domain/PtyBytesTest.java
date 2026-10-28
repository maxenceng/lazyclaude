package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PtyBytesTest {

    @Test
    void shouldBeEqualToBytesWithTheSameContent() {
        assertThat(ptyBytesOf("ls")).isEqualTo(ptyBytesOf("ls")).hasSameHashCodeAs(ptyBytesOf("ls"));
    }

    @Test
    void shouldDifferFromBytesWithOtherContent() {
        assertThat(ptyBytesOf("ls")).isNotEqualTo(ptyBytesOf("cd"));
    }

    @Test
    void shouldNotChangeWhenTheArrayItWasBuiltFromDoes() {
        byte[] source = {1, 2};
        PtyBytes bytes = new PtyBytes(source);

        source[0] = 9;

        assertThat(bytes.value()).containsExactly(1, 2);
    }

    @Test
    void shouldNotChangeWhenTheArrayItHandedOutDoes() {
        PtyBytes bytes = new PtyBytes(new byte[] {1, 2});

        bytes.value()[0] = 9;

        assertThat(bytes.value()).containsExactly(1, 2);
    }

    @Test
    void shouldBeEmptyOnlyWithoutBytes() {
        assertThat(new PtyBytes(new byte[0]).isEmpty()).isTrue();
        assertThat(ptyBytesOf("a").isEmpty()).isFalse();
    }
}
