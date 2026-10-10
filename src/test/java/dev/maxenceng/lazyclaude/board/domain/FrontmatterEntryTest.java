package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.frontmatterEntryBuilder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class FrontmatterEntryTest {

    @Test
    void shouldRefuseAnEntryWithoutAKey() {
        assertThatThrownBy(() -> frontmatterEntryBuilder().key(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseAnEntryWithoutAValue() {
        assertThatThrownBy(() -> frontmatterEntryBuilder().value(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }
}
