package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class BodyTest {

    @Test
    void shouldRefuseABlankBody() {
        assertThatThrownBy(() -> new Body(" \n"))
                .as("a body with nothing in it is NoBody, not an empty Body")
                .isInstanceOf(BlankValueException.class);
    }
}
