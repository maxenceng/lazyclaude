package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketDescriptionTest {

    @Test
    void shouldRefuseABlankDescription() {
        assertThatThrownBy(() -> new TicketDescription("   ")).isInstanceOf(BlankValueException.class);
    }

    @Test
    void shouldRefuseAMissingDescription() {
        assertThatThrownBy(() -> new TicketDescription(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldKeepQuotesAndSpacesAsTyped() {
        assertThat(new TicketDescription("  say \"hi\"  and 'bye'  ").value()).isEqualTo("  say \"hi\"  and 'bye'  ");
    }
}
