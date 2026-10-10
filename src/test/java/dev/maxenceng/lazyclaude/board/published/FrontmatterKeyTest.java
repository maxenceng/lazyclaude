package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class FrontmatterKeyTest {

    @Test
    void shouldRefuseABlankKey() {
        assertThatThrownBy(() -> new FrontmatterKey(" ")).isInstanceOf(BlankValueException.class);
    }
}
