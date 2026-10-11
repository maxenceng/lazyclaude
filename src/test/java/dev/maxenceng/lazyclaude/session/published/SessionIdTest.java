package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionIdTest {

    @Test
    void shouldShortenAnIdToItsFirstEightCharacters() {
        assertThat(sessionId().shortValue()).isEqualTo("70c67de2");
    }

    @Test
    void shouldKeepAnIdShorterThanEightCharactersWhole() {
        assertThat(new SessionId("abc").shortValue()).isEqualTo("abc");
    }
}
