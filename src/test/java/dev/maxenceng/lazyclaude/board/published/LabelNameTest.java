package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class LabelNameTest {

    @Test
    void shouldRefuseABlankLabel() {
        assertThatThrownBy(() -> new LabelName(" ")).isInstanceOf(BlankValueException.class);
    }
}
