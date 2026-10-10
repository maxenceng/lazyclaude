package dev.maxenceng.lazyclaude.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketId;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketTest {

    private static Ticket.TicketBuilder bare() {
        return Ticket.builder()
                .id(new TicketId("PROJ-1"))
                .title(new NoTitle())
                .status(Status.DRAFT)
                .context(new NoContext())
                .type(new NoType());
    }

    @Test
    void shouldKeepAnAbsentTitleContextAndTypeAsAbsent() {
        Ticket ticket = bare().build();

        assertThat(ticket.title()).as("the domain no longer stands the id in for a missing title").isEqualTo(new NoTitle());
        assertThat(ticket.context()).isEqualTo(new NoContext());
        assertThat(ticket.type()).isEqualTo(new NoType());
    }

    @Test
    void shouldRefuseATicketWithoutAnId() {
        assertThatThrownBy(() -> bare().id(null).build())
                .isInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("id");
    }

    @Test
    void shouldRefuseATicketWithoutAStatus() {
        assertThatThrownBy(() -> bare().status(null).build()).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseTitleIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> bare().title(null).build())
                .as("absence is NoTitle, never null")
                .isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseContextIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> bare().context(null).build()).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseTypeIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> bare().type(null).build()).isInstanceOf(MissingMandatoryValueException.class);
    }
}
