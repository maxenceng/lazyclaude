package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class TitleTest {

    @Test
    void shouldRefuseABlankTitle() {
        assertThatThrownBy(() -> new Title(" "))
                .as("a missing title is NoTitle, not an empty Title")
                .isInstanceOf(BlankValueException.class);
    }
}
