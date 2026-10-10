package dev.maxenceng.lazyclaude.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketIdTest {

    @Test
    void shouldOrderTwoBeforeTen() {
        assertThat(TicketId.of("PROJ-2")).isLessThan(TicketId.of("PROJ-10"));
    }

    @Test
    void shouldOrderByPrefixBeforeNumber() {
        assertThat(TicketId.of("PROJ-10")).isLessThan(TicketId.of("WIDGET-1"));
    }

    @Test
    void shouldTreatEqualTextAsEqualIds() {
        assertThat(TicketId.of("PROJ-20")).isEqualTo(TicketId.of("PROJ-20"));
    }

    @Test
    void shouldKeepTheTextItWasGiven() {
        assertThat(TicketId.of("PROJ-20").value()).isEqualTo("PROJ-20");
    }

    @Test
    void shouldRefuseAnIdWithoutANumber() {
        assertThatThrownBy(() -> TicketId.of("LAZY")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("LAZY");
    }

    @Test
    void shouldRefuseABlankId() {
        assertThatThrownBy(() -> TicketId.of(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRefuseANumberTooLargeToOrder() {
        assertThatThrownBy(() -> TicketId.of("PROJ-99999999999999999999")).isInstanceOf(IllegalArgumentException.class);
    }
}
