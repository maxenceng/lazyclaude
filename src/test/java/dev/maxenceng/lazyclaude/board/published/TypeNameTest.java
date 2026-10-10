package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class TypeNameTest {

    @Test
    void shouldRefuseABlankType() {
        assertThatThrownBy(() -> new TypeName("")).as("a missing type is NoType").isInstanceOf(BlankValueException.class);
    }
}
