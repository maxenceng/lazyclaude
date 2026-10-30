package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.GitHubIssueFixture.closedIssue;
import static dev.maxenceng.lazyclaude.board.domain.GitHubIssueFixture.openIssue;
import static dev.maxenceng.lazyclaude.board.domain.GitHubIssueFixture.todoLabels;
import static dev.maxenceng.lazyclaude.board.domain.GitHubIssueFixture.unreadable;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;

class GitHubIssueTest {

    @Test
    void shouldReadAnOpenIssueAsTheTicketItsStatusContextAndTypeLabelsName() {
        assertThat(openIssue(todoLabels()).entry())
                .usingRecursiveComparison()
                .isEqualTo(ticketBuilder()
                        .id(ticketId())
                        .branch(ticketBranch())
                        .status(Status.TODO)
                        .context(contextName())
                        .type(typeName())
                        .title(title())
                        .body(body())
                        .build());
    }

    @Test
    void shouldReadAnOpenIssueWithoutAStatusLabelAsAnUnassignedDraft() {
        assertThat(openIssue().entry()).isInstanceOfSatisfying(Ticket.class, ticket -> {
            assertThat(ticket.status()).isEqualTo(Status.DRAFT);
            assertThat(ticket.context()).isEqualTo(new NoContext());
            assertThat(ticket.type()).isEqualTo(new NoType());
        });
    }

    @Test
    void shouldReadADraftThatCarriesItsContextAndType() {
        assertThat(openIssue("status:draft", "context:board", "type:fix").entry()).isInstanceOfSatisfying(Ticket.class, ticket -> {
            assertThat(ticket.status()).isEqualTo(Status.DRAFT);
            assertThat(ticket.context()).isEqualTo(contextName());
        });
    }

    @Test
    void shouldRefuseADraftWithTwoContextLabelsSinceItWouldHaveNoSingleContext() {
        assertThat(openIssue("context:board", "context:docs").entry())
                .isEqualTo(unreadable("needs exactly one context: label, has several"));
    }

    @Test
    void shouldRefuseATicketPastDraftWithNoContextLabel() {
        assertThat(openIssue("status:todo", "type:feature").entry())
                .isEqualTo(unreadable("needs exactly one context: label, has none"));
    }

    @Test
    void shouldRefuseATicketPastDraftWithTwoContextLabels() {
        assertThat(openIssue("status:todo", "context:board", "context:docs", "type:feature").entry())
                .isEqualTo(unreadable("needs exactly one context: label, has several"));
    }

    @Test
    void shouldReadATicketPastDraftWithExactlyOneContextAndOneTypeLabel() {
        assertThat(openIssue("status:in-review", "context:board", "type:chore").entry()).isInstanceOf(Ticket.class);
    }

    @Test
    void shouldRefuseATicketPastDraftWithNoTypeLabel() {
        assertThat(openIssue("status:in-progress", "context:board").entry())
                .isEqualTo(unreadable("needs exactly one type: label, has none"));
    }

    @Test
    void shouldRefuseATicketPastDraftWithTwoTypeLabels() {
        assertThat(openIssue("status:todo", "context:board", "type:feature", "type:fix").entry())
                .isEqualTo(unreadable("needs exactly one type: label, has several"));
    }

    @Test
    void shouldRefuseAnIssueWithTwoStatusLabelsNamingBoth() {
        assertThat(openIssue("status:todo", "status:in-progress", "context:board", "type:feature").entry())
                .isEqualTo(unreadable("has more than one status label: todo, in-progress"));
    }

    @Test
    void shouldRefuseAStatusLabelTheLifecycleHasNoWordFor() {
        assertThat(openIssue("status:blocked", "context:board", "type:feature").entry())
                .isEqualTo(unreadable("status label 'blocked' is not one of draft, todo, in-progress, in-review, done"));
    }

    @Test
    void shouldReadAnIssueClosedAsCompletedAsDoneWhateverStatusLabelItStillCarries() {
        assertThat(closedIssue("COMPLETED", "status:in-review", "status:todo", "context:board", "type:feature").entry())
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.status()).isEqualTo(Status.DONE));
    }

    @Test
    void shouldReadAnIssueClosedWithoutAReasonAsDone() {
        assertThat(closedIssue("", "context:board", "type:feature").entry())
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.status()).isEqualTo(Status.DONE));
    }

    @Test
    void shouldRefuseAnIssueClosedAsNotPlanned() {
        assertThat(closedIssue("NOT_PLANNED", "context:board", "type:feature").entry()).isEqualTo(unreadable("closed as not planned"));
    }

    @Test
    void shouldRefuseAnIssueClosedAsADuplicate() {
        assertThat(closedIssue("DUPLICATE", "context:board", "type:feature").entry()).isEqualTo(unreadable("closed as duplicate"));
    }

    @Test
    void shouldReadAnIssueClosedAsCompletedWithoutItsContextOrTypeLabelAsDone() {
        assertThat(closedIssue("COMPLETED").entry())
                .as("a draft closed straight from GitHub is done, not an unreadable row for good (ADR 0052)")
                .isInstanceOfSatisfying(Ticket.class, ticket -> {
                    assertThat(ticket.status()).isEqualTo(Status.DONE);
                    assertThat(ticket.context()).isEqualTo(new NoContext());
                    assertThat(ticket.type()).isEqualTo(new NoType());
                });
    }

    @Test
    void shouldRefuseAnIssueClosedAsCompletedWithTwoContextLabelsSinceItWouldHaveNoSingleContext() {
        assertThat(closedIssue("COMPLETED", "context:board", "context:docs", "type:feature").entry())
                .isEqualTo(unreadable("needs exactly one context: label, has several"));
    }

    @Test
    void shouldKeepTheLabelsTheBoardDoesNotModelInTheOrderGitHubListsThem() {
        assertThat(openIssue("help wanted", "status:todo", "context:board", "type:feature", "bug").entry())
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.otherLabels())
                        .containsExactly(new LabelName("help wanted"), new LabelName("bug")));
    }
}
