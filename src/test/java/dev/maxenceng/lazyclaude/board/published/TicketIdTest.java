package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;
import dev.maxenceng.lazyclaude.error.domain.NotMatchingExpectedPatternException;

class TicketIdTest {

    @Test
    void shouldOrderTwoBeforeTen() {
        assertThat(new TicketId("2")).isLessThan(new TicketId("10"));
    }

    @Test
    void shouldTreatEqualTextAsEqualIds() {
        assertThat(new TicketId("20")).isEqualTo(new TicketId("20"));
    }

    @Test
    void shouldKeepTheTextItWasGiven() {
        assertThat(new TicketId("20").value()).isEqualTo("20");
    }

    @Test
    void shouldRefuseAnIdThatIsNotAnIssueNumber() {
        assertThatThrownBy(() -> new TicketId("PROJ-4"))
                .isInstanceOf(NotMatchingExpectedPatternException.class)
                .hasMessage("id 'PROJ-4' is not an issue number");
    }

    @Test
    void shouldRefuseZeroBecauseGitHubNumbersFromOne() {
        assertThatThrownBy(() -> new TicketId("0")).isInstanceOf(NotMatchingExpectedPatternException.class);
    }

    @Test
    void shouldRefuseANumberWithALeadingZero() {
        assertThatThrownBy(() -> new TicketId("07")).isInstanceOf(NotMatchingExpectedPatternException.class);
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
        assertThatThrownBy(() -> new TicketId("99999999999999999999")).isInstanceOf(NotMatchingExpectedPatternException.class);
    }
}
