package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AssertionException;

class TicketLinkTest {

    @Test
    void shouldFindTheLinkToATicketAmongSeveral() {
        assertThat(TicketLink.to(List.of(ticketLink("PROJ-1", "start"), ticketLink("PROJ-2", "review")), new LinkedTicketId("PROJ-2")))
                .contains(ticketLink("PROJ-2", "review"));
    }

    @Test
    void shouldFindNoLinkToATicketItIsNotLinkedTo() {
        assertThat(TicketLink.to(List.of(ticketLink("PROJ-1", "start")), new LinkedTicketId("PROJ-2"))).isEmpty();
    }

    @Test
    void shouldRefuseABlankTicketId() {
        assertThatThrownBy(() -> new LinkedTicketId(" ")).isInstanceOf(AssertionException.class);
    }

    @Test
    void shouldRefuseABlankVerb() {
        assertThatThrownBy(() -> new LinkVerb("")).isInstanceOf(AssertionException.class);
    }
}
