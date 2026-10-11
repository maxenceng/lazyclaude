package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.onBranches;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ranOnBranch;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.strong;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.weak;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkFactsTest {

    @Test
    void shouldLinkStronglyToATicketTheArgsNamed() {
        assertThat(ran(ticketRun("start", "PROJ-1")).linksTo(boardTickets())).containsExactly(strong("PROJ-1", "start"));
    }

    @Test
    void shouldMatchANamedIdCaseInsensitivelyAndSpellItAsTheBoardDoes() {
        assertThat(ran(ticketRun("start", "proj-1")).linksTo(boardTickets())).containsExactly(strong("PROJ-1", "start"));
    }

    @Test
    void shouldIgnoreTheBranchWhenTheArgsNamedATicket() {
        assertThat(ranOnBranch(ticketRun("start", "PROJ-1"), "PROJ-2").linksTo(boardTickets()))
                .as("the args are the user's own word for what the session is about, the branch only where it happened to run")
                .containsExactly(strong("PROJ-1", "start"));
    }

    @Test
    void shouldLinkNothingWhenEveryNamedIdMissesTheBoardEvenIfTheBranchNamesATicket() {
        assertThat(ranOnBranch(ticketRun("start", "GONE-9"), "PROJ-2").linksTo(boardTickets()))
                .as("a session whose args named only tickets the board lacks is unlinked, not linked by its branch")
                .isEmpty();
    }

    @Test
    void shouldLinkOnlyTheNamedIdsTheBoardHas() {
        assertThat(ran(ticketRun("review", "GONE-9", "PROJ-2")).linksTo(boardTickets())).containsExactly(strong("PROJ-2", "review"));
    }

    @Test
    void shouldCarryTheLastVerbRunOnATicket() {
        assertThat(ran(ticketRun("refine", "PROJ-1"), ticketRun("start", "PROJ-1")).linksTo(boardTickets()))
                .containsExactly(strong("PROJ-1", "start"));
    }

    @Test
    void shouldLinkToEveryTicketTheArgsNamed() {
        assertThat(ran(ticketRun("review", "PROJ-1", "PROJ-2")).linksTo(boardTickets()))
                .containsExactlyInAnyOrder(strong("PROJ-1", "review"), strong("PROJ-2", "review"));
    }

    @Test
    void shouldKeepTheVerbOfEachTicketWhenRunsNameDifferentOnes() {
        assertThat(ran(ticketRun("refine", "PROJ-1"), ticketRun("start", "PROJ-2")).linksTo(boardTickets()))
                .containsExactlyInAnyOrder(strong("PROJ-1", "refine"), strong("PROJ-2", "start"));
    }

    @Test
    void shouldLinkWeaklyThroughABranchWhenNoArgsNamedATicket() {
        assertThat(onBranches("PROJ-2").linksTo(boardTickets())).containsExactly(weak("PROJ-2"));
    }

    @Test
    void shouldMatchABranchIdCaseInsensitively() {
        assertThat(onBranches("proj-2").linksTo(boardTickets())).containsExactly(weak("PROJ-2"));
    }

    @Test
    void shouldLinkWeaklyToEveryTicketItsBranchesNamed() {
        assertThat(onBranches("PROJ-1", "PROJ-2", "PROJ-1").linksTo(boardTickets()))
                .containsExactlyInAnyOrder(weak("PROJ-1"), weak("PROJ-2"));
    }

    @Test
    void shouldLinkNothingThroughABranchTheBoardHasNoTicketFor() {
        assertThat(onBranches("OTHER-4").linksTo(boardTickets())).isEmpty();
    }

    @Test
    void shouldLinkNothingWithoutAnyFact() {
        assertThat(LinkFacts.none().linksTo(boardTickets())).as("main, or a branch with no ticket prefix, records none").isEmpty();
    }

    @Test
    void shouldLinkNothingWhenTheBoardHasNoTicket() {
        assertThat(ranOnBranch(ticketRun("start", "PROJ-1"), "PROJ-1").linksTo(noTickets())).isEmpty();
    }
}
