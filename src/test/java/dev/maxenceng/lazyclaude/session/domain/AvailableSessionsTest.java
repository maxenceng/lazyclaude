package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openLive;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.hostedLiveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;


class AvailableSessionsTest {

    @Test
    void shouldMarkALiveSessionHostedWhenItsPidIsOneLazyclaudeSpawned() {
        Sessions hosting = liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting).isEqualTo(availableSessionsHosting(List.of(hostedLiveSession()), List.of(openLive(hostedLiveSession()))));
    }

    @Test
    void shouldLeaveALiveSessionOutsideWhenItsPidIsNotOneLazyclaudeSpawned() {
        AvailableSessions hosting = liveSessionsOf(List.of(newestSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.live()).extracting(LiveSession::hosting).containsExactly(Hosting.OUTSIDE);
    }

    @Test
    void shouldListAHostedSessionAsStartingWhileTheRegistryHasNoRecordOfIt() {
        Sessions hosting = liveSessionsOf(List.of()).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting).isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }
}
