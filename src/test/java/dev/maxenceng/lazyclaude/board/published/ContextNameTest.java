package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class ContextNameTest {

    @Test
    void shouldRefuseABlankContext() {
        assertThatThrownBy(() -> new ContextName("")).as("a missing context is NoContext").isInstanceOf(BlankValueException.class);
    }
}
