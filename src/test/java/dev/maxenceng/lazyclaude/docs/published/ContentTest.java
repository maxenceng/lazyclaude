package dev.maxenceng.lazyclaude.docs.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class ContentTest {

    @Test
    void shouldRefuseBlankContent() {
        assertThatThrownBy(() -> new Content("  "))
                .as("a blank document is EmptyDocument, not empty Content")
                .isInstanceOf(BlankValueException.class);
    }
}
