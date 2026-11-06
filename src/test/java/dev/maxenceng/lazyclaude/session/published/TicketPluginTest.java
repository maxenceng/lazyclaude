package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;
import dev.maxenceng.lazyclaude.error.domain.NotMatchingExpectedPatternException;

class TicketPluginTest {

    @Test
    void shouldRefuseABlankPluginName() {
        assertThatThrownBy(() -> new TicketPlugin("  ")).isInstanceOf(BlankValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"my tools", "a:b", "/my-tools", " my-tools"})
    void shouldRefuseANameThatWouldSplitTheSlashCommand(String name) {
        assertThatThrownBy(() -> new TicketPlugin(name)).isInstanceOf(NotMatchingExpectedPatternException.class);
    }

    @Test
    void shouldRefuseAMissingPluginName() {
        assertThatThrownBy(() -> new TicketPlugin(null)).isInstanceOf(MissingMandatoryValueException.class);
    }
}
