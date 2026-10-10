package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;
import dev.maxenceng.lazyclaude.error.domain.NotMatchingExpectedPatternException;

class TicketIdTest {

    @Test
    void shouldOrderTwoBeforeTen() {
        assertThat(new TicketId("PROJ-2")).isLessThan(new TicketId("PROJ-10"));
    }

    @Test
    void shouldOrderByPrefixBeforeNumber() {
        assertThat(new TicketId("PROJ-10")).isLessThan(new TicketId("WIDGET-1"));
    }

    @Test
    void shouldTreatEqualTextAsEqualIds() {
        assertThat(new TicketId("PROJ-20")).isEqualTo(new TicketId("PROJ-20"));
    }

    @Test
    void shouldKeepTheTextItWasGiven() {
        assertThat(new TicketId("PROJ-20").value()).isEqualTo("PROJ-20");
    }

    @Test
    void shouldRefuseAnIdWithoutANumber() {
        assertThatThrownBy(() -> new TicketId("LAZY"))
                .isInstanceOf(NotMatchingExpectedPatternException.class)
                .hasMessage("id 'LAZY' is not PREFIX-number");
    }

    @Test
    void shouldRefuseABlankId() {
        assertThatThrownBy(() -> new TicketId(" ")).isInstanceOf(NotMatchingExpectedPatternException.class);
    }

    @Test
    void shouldRefuseAMissingId() {
        assertThatThrownBy(() -> new TicketId(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseANumberTooLargeToOrder() {
        assertThatThrownBy(() -> new TicketId("PROJ-99999999999999999999"))
                .isInstanceOf(NotMatchingExpectedPatternException.class);
    }
}
