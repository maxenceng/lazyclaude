package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LastKnownTicketsTest {

    @Test
    void shouldHoldNoTicketsBeforeItIsTold() {
        assertThat(new LastKnownTickets().current()).isEqualTo(noTickets());
    }

    @Test
    void shouldGiveBackTheTicketsItWasLastToldOf() {
        LastKnownTickets tickets = new LastKnownTickets();
        tickets.know(noTickets());

        tickets.know(boardTickets());

        assertThat(tickets.current()).as("a newer board replaces the one before it").isEqualTo(boardTickets());
    }
}
