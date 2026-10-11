package dev.maxenceng.lazyclaude.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class WarningTest {

    @Test
    void shouldKeepItsText() {
        assertThat(new Warning("cannot watch docs/").text()).isEqualTo("cannot watch docs/");
    }

    @Test
    void shouldRefuseABlankText() {
        assertThatThrownBy(() -> new Warning(" ")).isInstanceOf(BlankValueException.class);
    }

    @Test
    void shouldRefuseAMissingText() {
        assertThatThrownBy(() -> new Warning(null)).isInstanceOf(MissingMandatoryValueException.class);
    }
}
