package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openLive;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.hostedLiveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.strongLink;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.weakLink;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;

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

    @Test
    void shouldLinkALiveHostedSessionToTheTicketItWasStartedFor() {
        HostedSession started = hostedSessionRunning(Verb.REVIEW);
        AvailableSessions hosting =
                liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(started));

        assertThat(hosting.live())
                .extracting(LiveSession::links)
                .as("its transcript has not linked it yet, and it must not drop to the unlinked section meanwhile")
                .containsExactly(List.of(strongLink("PROJ-1", "review")));
    }

    @Test
    void shouldKeepTheLinkItsTranscriptAlreadyGivesToThatTicket() {
        LiveSession linked = liveSessionBuilder().links(List.of(strongLink("PROJ-1", "respond"))).build();

        AvailableSessions hosting = liveSessionsOf(List.of(linked)).hosting(hostedSessionsOf(hostedSessionRunning(Verb.REVIEW)));

        assertThat(hosting.live()).extracting(LiveSession::links).containsExactly(List.of(strongLink("PROJ-1", "respond")));
    }

    @Test
    void shouldLinkNoLiveSessionThatIsNotHostedForATicket() {
        AvailableSessions hosting = liveSessionsOf(List.of(liveSession())).hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting.live()).extracting(LiveSession::links).containsExactly(List.of());
    }

    @Test
    void shouldHaveRunAVerbWhenAPastSessionLastRanItOnTheTicket() {
        PastSession design = pastSessionBuilder().links(List.of(strongLink("PROJ-1", "design"))).build();

        assertThat(pastSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunAVerbWhenALiveSessionLastRanItOnTheTicket() {
        LiveSession design = liveSessionBuilder().links(List.of(strongLink("PROJ-1", "design"))).build();

        assertThat(liveSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunAVerbWhenAHostedSessionStillStartingWasLaunchedWithIt() {
        AvailableSessions hosting = liveSessionsOf(List.of()).hosting(hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(hosting.ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldNotHaveRunAVerbAnotherTicketsSessionRan() {
        PastSession design = pastSessionBuilder().links(List.of(strongLink("PROJ-2", "design"))).build();

        assertThat(pastSessionsOf(List.of(design)).ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldNotHaveRunAVerbAWeakLinkNamesNoVerbFor() {
        PastSession weak = pastSessionBuilder().links(List.of(weakLink("PROJ-1"))).build();

        assertThat(pastSessionsOf(List.of(weak)).ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }
}
