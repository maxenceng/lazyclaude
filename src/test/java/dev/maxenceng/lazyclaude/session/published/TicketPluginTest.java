package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketPluginTest {

    @Test
    void shouldRefuseABlankPluginName() {
        assertThatThrownBy(() -> new TicketPlugin("  ")).isInstanceOf(BlankValueException.class);
    }

    @Test
    void shouldRefuseAMissingPluginName() {
        assertThatThrownBy(() -> new TicketPlugin(null)).isInstanceOf(MissingMandatoryValueException.class);
    }
}
