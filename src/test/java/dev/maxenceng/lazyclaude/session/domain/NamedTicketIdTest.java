package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamedTicketIdTest {

    @Test
    void shouldReadAWordShapedLikeATicketIdInAnyCase() {
        assertThat(NamedTicketId.fromWord("pay-7")).contains(new NamedTicketId("pay-7"));
        assertThat(NamedTicketId.fromWord("V2-4")).as("the board accepts digits after the first letter").contains(new NamedTicketId("V2-4"));
    }

    @Test
    void shouldNotReadAWordThatIsNotShapedLikeATicketId() {
        assertThat(NamedTicketId.fromWord("start")).isEmpty();
        assertThat(NamedTicketId.fromWord("PAY-7-slug")).as("a whole word must be the id, not start with it").isEmpty();
        assertThat(NamedTicketId.fromWord("2-PAY")).isEmpty();
    }
}
