package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GhContractFixture.aRepositoryWithIssues;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GhContractFixture.issuesOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.GhContractFixture.thisRepository;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.Ticket;

@Tag("contract")
class GhContractTest {

    @Test
    void shouldReadTheIssuesGhIssueListPrintsForARealRepositoryWithSomeAsTickets() {
        assertThat(issuesOf(aRepositoryWithIssues()))
                .as("gh issue list --json must still write the fields the board reads")
                .isNotEmpty()
                .anySatisfy(entry -> assertThat(entry).isInstanceOf(Ticket.class));
    }

    @Test
    void shouldAnswerNotModifiedToAConditionalRequestOnTheEtagItJustGave() {
        IssuesWatchRepository watcher = new IssuesWatchRepository(new GhCommand("gh", Duration.ofSeconds(30)), Duration.ofSeconds(30));

        assertThat(watcher.changedSinceLastCheck(thisRepository())).as("the first check only takes the etag").isFalse();
        assertThat(watcher.changedSinceLastCheck(thisRepository()))
                .as("a 304 from gh api --include --silent must still read as no change")
                .isFalse();
    }
}
