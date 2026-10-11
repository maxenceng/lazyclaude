package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionOfTheLiveOne;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AvailableSessionsTest {

    @Test
    void shouldRefuseASessionThatIsBothLiveAndPast() {
        assertThatThrownBy(() -> availableSessionsOf(List.of(liveSession()), List.of(pastSessionOfTheLiveOne())))
                .as("the panel keys rows by session id, so a session listed twice would leave one of its rows unselectable")
                .isInstanceOf(SessionListedTwiceException.class);
    }

    @Test
    void shouldHoldALiveAndADifferentPastSession() {
        assertThat(availableSessionsOf(List.of(liveSession()), List.of(pastSession())).past()).containsExactly(pastSession());
    }
}
