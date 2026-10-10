package dev.maxenceng.lazyclaude.board.published;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketBodyTest {

    @Test
    void shouldTakeAnEmptyTextForNoBody() {
        assertThat(TicketBody.of("")).isEqualTo(new NoBody());
    }

    @Test
    void shouldTakeAWhitespaceOnlyTextForNoBody() {
        assertThat(TicketBody.of(" \n\t\n  "))
                .as("whitespace renders as a blank, so it is as good as no body")
                .isEqualTo(new NoBody());
    }

    @Test
    void shouldKeepTheTextOfANonBlankBodyAsWritten() {
        assertThat(TicketBody.of("\n# Title\n\nText\n")).isEqualTo(new Body("\n# Title\n\nText\n"));
    }

    @Test
    void shouldKeepAThematicBreakInTheBody() {
        assertThat(TicketBody.of("one\n\n---\n\ntwo\n")).isEqualTo(new Body("one\n\n---\n\ntwo\n"));
    }
}
