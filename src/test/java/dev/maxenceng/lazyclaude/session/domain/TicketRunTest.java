package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AssertionException;

class TicketRunTest {

    @Test
    void shouldRejectARunThatNamesNoId() {
        assertThatThrownBy(() -> ticketRun("start"))
                .as("a run that names no id would make the args win over the branch while linking nothing")
                .isInstanceOf(AssertionException.class);
    }
}
