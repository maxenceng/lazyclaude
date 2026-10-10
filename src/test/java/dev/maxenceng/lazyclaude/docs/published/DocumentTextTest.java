package dev.maxenceng.lazyclaude.docs.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class DocumentTextTest {

    @Test
    void shouldBeEmptyWhenTheFileHoldsNothing() {
        assertThat(DocumentText.of("")).isEqualTo(new EmptyDocument());
    }

    @Test
    void shouldBeEmptyWhenTheFileHoldsOnlyWhitespace() {
        assertThat(DocumentText.of(" \n\t\n")).isEqualTo(new EmptyDocument());
    }

    @Test
    void shouldBeContentWhenTheFileHoldsText() {
        assertThat(DocumentText.of("# Title\n")).isEqualTo(new Content("# Title\n"));
    }

    @Test
    void shouldRefuseBlankContent() {
        assertThatThrownBy(() -> new Content("  "))
                .as("a blank document is EmptyDocument, not empty Content")
                .isInstanceOf(BlankValueException.class);
    }
}
