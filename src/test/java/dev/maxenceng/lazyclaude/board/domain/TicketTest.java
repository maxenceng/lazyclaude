package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.bareTicket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketTest {

    @Test
    void shouldKeepAnAbsentTitleContextAndTypeAsAbsent() {
        Ticket ticket = bareTicket();

        assertThat(ticket.title()).as("the domain no longer stands the id in for a missing title").isEqualTo(new NoTitle());
        assertThat(ticket.context()).isEqualTo(new NoContext());
        assertThat(ticket.type()).isEqualTo(new NoType());
    }

    @Test
    void shouldRefuseATicketWithoutAnId() {
        assertThatThrownBy(() -> ticketBuilder().id(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("id");
    }

    @Test
    void shouldRefuseATicketWithoutAStatus() {
        assertThatThrownBy(() -> ticketBuilder().status(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseTitleIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().title(null).build())
                .as("absence is NoTitle, never null")
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseContextIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().context(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseTypeIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().type(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }
}
