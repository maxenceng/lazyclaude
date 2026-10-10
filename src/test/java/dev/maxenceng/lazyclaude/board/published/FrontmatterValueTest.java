package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class FrontmatterValueTest {

    @Test
    void shouldRefuseAMissingValue() {
        assertThatThrownBy(() -> new FrontmatterValue(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldAcceptAnEmptyValue() {
        assertThat(new FrontmatterValue("").text()).as("a key can be written with nothing after it").isEmpty();
    }
}
