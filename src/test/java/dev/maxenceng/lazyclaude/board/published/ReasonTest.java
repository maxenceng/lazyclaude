package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class ReasonTest {

    @Test
    void shouldRefuseAMissingReason() {
        assertThatThrownBy(() -> new Reason(null))
                .as("an unreadable file is never listed without saying why")
                .isInstanceOf(MissingMandatoryValueException.class);
    }
}
