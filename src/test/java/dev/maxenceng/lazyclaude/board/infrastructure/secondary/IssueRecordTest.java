package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.bodyOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.branchOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.closedIssue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueTitled;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueWithBody;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.openIssue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.todoLabels;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.unreadable;
import static dev.maxenceng.lazyclaude.board.published.ContextNameFixture.contextName;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static dev.maxenceng.lazyclaude.board.published.TypeNameFixture.typeName;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoContext;
import dev.maxenceng.lazyclaude.board.published.NoType;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;

class IssueRecordTest {

    @Test
    void shouldReadAnOpenIssueAsTheTicketItsStatusContextAndTypeLabelsName() {
        assertThat(openIssue(todoLabels()).entry())
                .usingRecursiveComparison()
                .isEqualTo(ticketBuilder()
                        .id(ticketId())
                        .branch(new TicketBranch("1-a-board-from-the-backlog"))
                        .status(Status.TODO)
                        .context(contextName())
                        .type(typeName())
                        .title(title())
                        .body(new Body("Body."))
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
        assertThat(closedIssue("COMPLETED", "status:in-review", "context:board", "type:feature").entry())
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
    void shouldRefuseAnIssueClosedAsCompletedThatLacksItsContextLabel() {
        assertThat(closedIssue("COMPLETED", "type:feature").entry())
                .as("done is past draft, so it needs its context and type like any ticket")
                .isEqualTo(unreadable("needs exactly one context: label, has none"));
    }

    @Test
    void shouldKeepTheLabelsTheBoardDoesNotModelInTheOrderGitHubListsThem() {
        assertThat(openIssue("help wanted", "status:todo", "context:board", "type:feature", "bug").entry())
                .isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.otherLabels())
                        .containsExactly(new LabelName("help wanted"), new LabelName("bug")));
    }

    @Test
    void shouldNameTheBranchForTheNumberAndTheSlugOfTheTitle() {
        assertThat(branchOf(issueTitled("Hello, World: v1.2 (draft)!"))).isEqualTo(new TicketBranch("1-hello-world-v1-2-draft"));
    }

    @Test
    void shouldNameTheBranchForTheNumberAloneWhenTheTitleHasNothingToSlug() {
        assertThat(branchOf(issueTitled("¿¡!?"))).isEqualTo(new TicketBranch("1"));
    }

    @Test
    void shouldReadABodyAsItIsWritten() {
        assertThat(bodyOf(issueWithBody("## Acceptance criteria\n\n- [ ] One"))).isEqualTo(new Body("## Acceptance criteria\n\n- [ ] One"));
    }

    @Test
    void shouldReadAnEmptyBodyAsNoBody() {
        assertThat(bodyOf(issueWithBody(""))).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadAWhitespaceOnlyBodyAsNoBody() {
        assertThat(bodyOf(issueWithBody(" \n\t"))).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadAMissingBodyAsNoBody() {
        assertThat(bodyOf(issueWithBody(null))).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadTheNumberAsTheTicketsId() {
        assertThat(issueNumbered(42, "t", "OPEN").entry().id().value()).isEqualTo("42");
    }

    @Test
    void shouldReadAnIssueWithoutALabelsFieldAsHavingNone() {
        assertThat(new IssueRecord(1, "t", "b", null, "OPEN", "").entry()).isInstanceOf(Ticket.class);
    }

    @Test
    void shouldNameTheFieldsAnIssueLacks() {
        assertThat(new IssueRecord(null, " ", "b", List.of(), null, "").missingFields().listed()).isEqualTo("number, title, state");
    }

    @Test
    void shouldNameNoFieldMissingOfACompleteIssue() {
        assertThat(issue("OPEN", "").missingFields().any()).isFalse();
    }

    @Test
    void shouldKeepTheTitleAsTheIssueWritesIt() {
        assertThat(openIssue().entry()).isInstanceOfSatisfying(Ticket.class, ticket -> assertThat(ticket.title()).isEqualTo(title()));
    }
}
