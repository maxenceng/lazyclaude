package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noRuns;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LinkFactsTest {

    @Test
    void shouldLinkToATicketTheArgsNamed() {
        assertThat(ran(ticketRun("start", "1")).linksTo(boardTickets())).containsExactly(ticketLink("1", "start"));
    }

    @Test
    void shouldLinkOnlyTheNamedIdsTheBoardHas() {
        assertThat(ran(ticketRun("review", "9", "2")).linksTo(boardTickets())).containsExactly(ticketLink("2", "review"));
    }

    @Test
    void shouldCarryTheLastVerbRunOnATicket() {
        assertThat(ran(ticketRun("refine", "1"), ticketRun("start", "1")).linksTo(boardTickets()))
                .containsExactly(ticketLink("1", "start"));
    }

    @Test
    void shouldLinkToEveryTicketTheArgsNamed() {
        assertThat(ran(ticketRun("review", "1", "2")).linksTo(boardTickets()))
                .containsExactlyInAnyOrder(ticketLink("1", "review"), ticketLink("2", "review"));
    }

    @Test
    void shouldKeepTheVerbOfEachTicketWhenRunsNameDifferentOnes() {
        assertThat(ran(ticketRun("refine", "1"), ticketRun("start", "2")).linksTo(boardTickets()))
                .containsExactlyInAnyOrder(ticketLink("1", "refine"), ticketLink("2", "start"));
    }

    @Test
    void shouldLinkNothingWithoutAnyRun() {
        assertThat(noRuns().linksTo(boardTickets())).isEmpty();
    }

    @Test
    void shouldLinkNothingWhenTheBoardHasNoTicket() {
        assertThat(ran(ticketRun("start", "1")).linksTo(noTickets())).isEmpty();
    }

    @Test
    void shouldLinkALaunchFromTheFirstFrameBeforeTheTranscriptSaysAnything() {
        assertThat(noRuns().launchedAs(Optional.of(ticketRun("design", "1"))).linksTo(boardTickets()))
                .containsExactly(ticketLink("1", "design"));
    }

    @Test
    void shouldMoveTheLaunchVerbWhenALaterRunInTheTranscriptNamesTheSameTicket() {
        assertThat(ran(ticketRun("start", "1")).launchedAs(Optional.of(ticketRun("design", "1"))).linksTo(boardTickets()))
                .as("the transcript's run comes after the launch that began the session")
                .containsExactly(ticketLink("1", "start"));
    }

    @Test
    void shouldAddALinkWhenALaterRunNamesAnotherTicket() {
        assertThat(ran(ticketRun("review", "2")).launchedAs(Optional.of(ticketRun("design", "1"))).linksTo(boardTickets()))
                .containsExactly(ticketLink("1", "design"), ticketLink("2", "review"));
    }

    @Test
    void shouldAddNothingWhenTheSessionWasNotLaunchedOnATicket() {
        assertThat(ran(ticketRun("start", "1")).launchedAs(Optional.empty())).isEqualTo(ran(ticketRun("start", "1")));
    }
}
