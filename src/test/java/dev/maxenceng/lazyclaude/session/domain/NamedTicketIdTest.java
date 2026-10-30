package dev.maxenceng.lazyclaude.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamedTicketIdTest {

    @Test
    void shouldReadAWordThatIsAnIssueNumber() {
        assertThat(NamedTicketId.fromWord("7")).contains(new NamedTicketId("7"));
    }

    @Test
    void shouldReadAnIssueNumberWrittenWithItsNumberSign() {
        assertThat(NamedTicketId.fromWord("#7")).as("the number sign is how GitHub writes one").contains(new NamedTicketId("7"));
    }

    @Test
    void shouldNotReadAWordThatIsNotAnIssueNumber() {
        assertThat(NamedTicketId.fromWord("start")).isEmpty();
        assertThat(NamedTicketId.fromWord("7-slug")).as("a whole word must be the number").isEmpty();
        assertThat(NamedTicketId.fromWord("PAY-7")).isEmpty();
        assertThat(NamedTicketId.fromWord("0")).as("GitHub numbers from 1").isEmpty();
        assertThat(NamedTicketId.fromWord("##7")).isEmpty();
    }
}
