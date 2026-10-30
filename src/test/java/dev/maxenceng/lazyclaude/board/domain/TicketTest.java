package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.backendChanges;
import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.frontendChanges;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.bareTicket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class TicketTest {

    @Test
    void shouldKeepAnAbsentContextAndTypeAsAbsent() {
        Ticket ticket = bareTicket();

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
    void shouldRefuseATicketWithoutABranch() {
        assertThatThrownBy(() -> ticketBuilder().branch(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("branch");
    }

    @Test
    void shouldRefuseATicketWithoutAStatus() {
        assertThatThrownBy(() -> ticketBuilder().status(null).build())
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

    @Test
    void shouldRefuseATicketWithoutATitle() {
        assertThatThrownBy(() -> ticketBuilder().title(null).build())
                .isExactlyInstanceOf(MissingMandatoryValueException.class)
                .hasMessageContaining("title");
    }

    @Test
    void shouldRefuseATicketWithoutLabels() {
        assertThatThrownBy(() -> ticketBuilder().labels(null).build())
                .as("no labels is an empty list, never null")
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseATicketWhoseBodyIsNullRatherThanAbsent() {
        assertThatThrownBy(() -> ticketBuilder().body(null).build())
                .as("absence is NoBody, never null")
                .isExactlyInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldKeepAnAbsentBodyAsAbsent() {
        assertThat(bareTicket().body()).isEqualTo(new NoBody());
    }

    @Test
    void shouldOfferDesignAfterReviewOnAnInProgressTicketWhoseBranchTouchesTheFrontend() {
        Ticket ticket = ticketBuilder().status(Status.IN_PROGRESS).changes(frontendChanges()).build();

        assertThat(ticket.verbs()).containsExactly(Verb.REVIEW, Verb.DESIGN);
    }

    @Test
    void shouldNotOfferDesignWhenTheBranchLeavesTheFrontendAlone() {
        Ticket ticket = ticketBuilder().status(Status.IN_PROGRESS).changes(backendChanges()).build();

        assertThat(ticket.verbs()).containsExactly(Verb.REVIEW);
    }

    @Test
    void shouldNotOfferDesignWhenTheBranchCannotBeRead() {
        Ticket ticket = ticketBuilder().status(Status.IN_PROGRESS).changes(new UnreadChanges()).build();

        assertThat(ticket.verbs()).containsExactly(Verb.REVIEW);
    }

    @Test
    void shouldOfferDesignOnlyInProgressWhateverTheBranchTouches() {
        assertThat(List.of(Status.values()))
                .filteredOn(status -> status != Status.IN_PROGRESS)
                .allSatisfy(status -> assertThat(
                                ticketBuilder().status(status).changes(frontendChanges()).build().verbs())
                        .isEqualTo(status.verbs()));
    }

    @Test
    void shouldOfferNoVerbOnADoneTicket() {
        assertThat(ticketBuilder().status(Status.DONE).build().verbs()).isEmpty();
    }

    @Test
    void shouldReadTheChangesOfAnInProgressTicketOnly() {
        assertThat(List.of(Status.values()))
                .filteredOn(status -> ticketBuilder().status(status).build().mayOfferDesign())
                .containsExactly(Status.IN_PROGRESS);
    }

    @Test
    void shouldKeepTheTicketWhenGivenItsChanges() {
        assertThat(ticket().withChanges(frontendChanges()))
                .usingRecursiveComparison()
                .isEqualTo(ticketBuilder().changes(frontendChanges()).build());
    }
}
