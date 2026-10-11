package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamedTicketIdTest {

    @Test
    void shouldReadAWordShapedLikeATicketIdInAnyCase() {
        assertThat(NamedTicketId.fromWord("session-3")).contains(new NamedTicketId("session-3"));
        assertThat(NamedTicketId.fromWord("V2-4")).as("the board accepts digits after the first letter").contains(new NamedTicketId("V2-4"));
    }

    @Test
    void shouldNotReadAWordThatIsNotShapedLikeATicketId() {
        assertThat(NamedTicketId.fromWord("start")).isEmpty();
        assertThat(NamedTicketId.fromWord("SESSION-3-slug")).as("a whole word must be the id, not start with it").isEmpty();
        assertThat(NamedTicketId.fromWord("2-SESSION")).isEmpty();
    }

    @Test
    void shouldReadTheTicketIdABranchStartsWith() {
        assertThat(NamedTicketId.prefixOf("SESSION-3-ticket-sessions")).contains(new NamedTicketId("SESSION-3"));
        assertThat(NamedTicketId.prefixOf("V2-4")).contains(new NamedTicketId("V2-4"));
    }

    @Test
    void shouldReadNoTicketIdFromABranchThatDoesNotStartWithOne() {
        assertThat(NamedTicketId.prefixOf("main")).isEmpty();
        assertThat(NamedTicketId.prefixOf("feature/SESSION-3")).isEmpty();
        assertThat(NamedTicketId.prefixOf("SESSION-1234567890123456789"))
                .as("a number longer than the board's would otherwise be cut to a different ticket")
                .isEmpty();
    }
}
