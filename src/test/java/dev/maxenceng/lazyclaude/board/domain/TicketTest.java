package dev.maxenceng.lazyclaude.board.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketTest {

    @Test
    void shouldCarryWhatItWasBuiltWith() {
        Ticket ticket = Ticket.builder()
                .id(new TicketId("PROJ-1"))
                .title("A board")
                .status(Status.TODO)
                .context("board")
                .type("feature")
                .build();

        assertThat(ticket.title()).isEqualTo("A board");
        assertThat(ticket.status()).isEqualTo(Status.TODO);
        assertThat(ticket.context()).isEqualTo("board");
        assertThat(ticket.type()).isEqualTo("feature");
    }

    @Test
    void shouldShowItsIdAsTitleWhenItHasNone() {
        Ticket ticket = Ticket.builder()
                .id(new TicketId("PROJ-1"))
                .status(Status.DRAFT)
                .build();

        assertThat(ticket.title()).as("a ticket without a heading is still a ticket").isEqualTo("PROJ-1");
    }

    @Test
    void shouldShowItsIdAsTitleWhenTheTitleIsBlank() {
        Ticket ticket = Ticket.builder()
                .id(new TicketId("PROJ-1"))
                .title("  ")
                .status(Status.DRAFT)
                .build();

        assertThat(ticket.title()).isEqualTo("PROJ-1");
    }

    @Test
    void shouldHaveEmptyContextAndTypeWhenFrontmatterLacksThem() {
        Ticket ticket = Ticket.builder().id(new TicketId("PROJ-1")).status(Status.DRAFT).build();

        assertThat(ticket.context()).isEmpty();
        assertThat(ticket.type()).isEmpty();
    }

    @Test
    void shouldRefuseATicketWithoutAnId() {
        assertThatThrownBy(() -> Ticket.builder().status(Status.DRAFT).build()).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWithoutAStatus() {
        assertThatThrownBy(() -> Ticket.builder().id(new TicketId("PROJ-1")).build())
                .isInstanceOf(MissingMandatoryValueException.class);
    }
}
