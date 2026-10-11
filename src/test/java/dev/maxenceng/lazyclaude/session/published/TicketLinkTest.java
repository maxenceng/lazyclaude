package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AssertionException;

class TicketLinkTest {

    @Test
    void shouldNameTheTicketOfAStrongLink() {
        assertThat(new StrongLink(new LinkedTicketId("PROJ-1"), new LinkVerb("start")).ticket())
                .isEqualTo(new LinkedTicketId("PROJ-1"));
    }

    @Test
    void shouldNameTheTicketOfAWeakLink() {
        assertThat(new WeakLink(new LinkedTicketId("PROJ-1")).ticket()).isEqualTo(new LinkedTicketId("PROJ-1"));
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
