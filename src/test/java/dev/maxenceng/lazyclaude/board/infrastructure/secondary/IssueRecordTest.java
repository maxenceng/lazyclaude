package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.ClosedAtFixture.closedAt;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.closedIssue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueLinkedTo;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueTitled;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueTitledLinkedTo;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueWith;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueWithBody;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.labelsOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.linkedBranchesAbsent;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.openIssue;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.ClosedIssue;
import dev.maxenceng.lazyclaude.board.domain.ClosedReason;
import dev.maxenceng.lazyclaude.board.domain.OpenIssue;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;

class IssueRecordTest {

    @Test
    void shouldReadAnOpenIssueAsOpen() {
        assertThat(openIssue().issue().state()).isEqualTo(new OpenIssue());
    }

    @Test
    void shouldReadAClosedIssueWithTheReasonGitHubGives() {
        assertThat(closedIssue("NOT_PLANNED").issue().state()).isEqualTo(ClosedIssue.builder().reason(new ClosedReason("NOT_PLANNED")).closedAt(closedAt()).build());
    }

    @Test
    void shouldReadAClosedIssueWithoutAStateReasonFieldAsClosedWithNoReason() {
        assertThat(issueWith(1, "t", labelsOf(), "CLOSED", null, "2026-10-01T09:00:00Z").issue().state())
                .isEqualTo(ClosedIssue.builder().reason(new ClosedReason("")).closedAt(closedAt()).build());
    }

    @Test
    void shouldNameClosedAtAsMissingFromAClosedIssueThatLacksIt() {
        assertThat(issueWith(1, "t", labelsOf(), "CLOSED", "COMPLETED", null).missingFields().listed()).isEqualTo("closedAt");
    }

    @Test
    void shouldNotAskAnOpenIssueForClosedAt() {
        assertThat(issueWith(1, "t", labelsOf(), "OPEN", "", null).missingFields().any()).isFalse();
    }

    @Test
    void shouldRefuseAClosedAtThatIsNotATime() {
        assertThatThrownBy(() -> issueWith(1, "t", labelsOf(), "CLOSED", "COMPLETED", "yesterday").issue())
                .isInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh api graphql wrote a closedAt that is not a time: yesterday");
    }

    @Test
    void shouldReadTheLabelsInTheOrderGitHubListsThem() {
        assertThat(openIssue("status:todo", "help wanted").issue().labels())
                .containsExactly(new LabelName("status:todo"), new LabelName("help wanted"));
    }

    @Test
    void shouldDropALabelWrittenBlank() {
        assertThat(issueWith(1, "t", labelsOf(" ", "bug"), "OPEN", "", null).issue().labels()).containsExactly(new LabelName("bug"));
    }

    @Test
    void shouldNameTheBranchForTheNumberAndTheSlugOfTheTitle() {
        assertThat(issueTitled("Hello, World: v1.2 (draft)!").issue().branch()).isEqualTo(new TicketBranch("1-hello-world-v1-2-draft"));
    }

    @Test
    void shouldNameTheBranchForTheNumberAloneWhenTheTitleHasNothingToSlug() {
        assertThat(issueTitled("¿¡!?").issue().branch()).isEqualTo(new TicketBranch("1"));
    }

    @Test
    void shouldTakeTheBranchGitHubLinksWhenItDiffersFromTheTitleSlug() {
        assertThat(issueTitledLinkedTo("The ticket's branch", "1-the-tickets-branch").issue().branch())
                .as("GitHub drops the apostrophe where the slug turns it into a dash")
                .isEqualTo(new TicketBranch("1-the-tickets-branch"));
    }

    @Test
    void shouldTakeTheFirstBranchGitHubListsWhenTwoAreLinked() {
        assertThat(issueTitledLinkedTo("Two branches", "1-first-linked", "1-second-linked").issue().branch())
                .isEqualTo(new TicketBranch("1-first-linked"));
    }

    @Test
    void shouldFallBackToTheSlugWhenNoBranchIsLinked() {
        assertThat(issueTitledLinkedTo("Nothing linked").issue().branch()).isEqualTo(new TicketBranch("1-nothing-linked"));
    }

    @Test
    void shouldFallBackToTheNumberWhenNoBranchIsLinkedAndTheTitleHasNothingToSlug() {
        assertThat(issueTitledLinkedTo("¿¡!?").issue().branch()).isEqualTo(new TicketBranch("1"));
    }

    @Test
    void shouldReadAnIssueWithNoLinkedBranchesFieldAsNothingLinked() {
        assertThat(issueLinkedTo(linkedBranchesAbsent()).issue().branch()).isEqualTo(new TicketBranch("1-a-board-from-the-backlog"));
    }

    @Test
    void shouldReadAnIssueWhoseLinkedBranchesFieldIsNullAsNothingLinked() {
        assertThat(issueLinkedTo(null).issue().branch()).isEqualTo(new TicketBranch("1-a-board-from-the-backlog"));
    }

    @Test
    void shouldReadALinkedBranchWithNoRefAsNothingLinked() {
        assertThat(issueLinkedTo(new NodesRecord<>(List.of(new LinkedBranchRecord(null)))).issue().branch())
                .as("GitHub writes a null ref for a branch that was deleted")
                .isEqualTo(new TicketBranch("1-a-board-from-the-backlog"));
    }

    @Test
    void shouldSkipALinkedBranchWrittenAsNull() {
        assertThat(issueLinkedTo(new NodesRecord<>(Arrays.asList(null, new LinkedBranchRecord(new RefRecord("1-linked"))))).issue().branch())
                .isEqualTo(new TicketBranch("1-linked"));
    }

    @Test
    void shouldReadABodyAsItIsWritten() {
        assertThat(issueWithBody("## Acceptance criteria\n\n- [ ] One").issue().body()).isEqualTo(new Body("## Acceptance criteria\n\n- [ ] One"));
    }

    @Test
    void shouldReadAnEmptyBodyAsNoBody() {
        assertThat(issueWithBody("").issue().body()).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadAWhitespaceOnlyBodyAsNoBody() {
        assertThat(issueWithBody(" \n\t").issue().body()).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadAMissingBodyAsNoBody() {
        assertThat(issueWithBody(null).issue().body()).isEqualTo(new NoBody());
    }

    @Test
    void shouldReadTheNumberAsTheTicketsId() {
        assertThat(issueNumbered(42, "t", "OPEN").issue().id().value()).isEqualTo("42");
    }

    @Test
    void shouldReadAnIssueWithoutALabelsFieldAsHavingNone() {
        assertThat(issueWith(1, "t", null, "OPEN", "", null).issue().labels()).isEmpty();
    }

    @Test
    void shouldNameTheFieldsAnIssueLacks() {
        assertThat(issueWith(null, " ", labelsOf(), null, "", null).missingFields().listed()).isEqualTo("number, title, state");
    }

    @Test
    void shouldNameNoFieldMissingOfACompleteIssue() {
        assertThat(issue("OPEN", "").missingFields().any()).isFalse();
    }

    @Test
    void shouldKeepTheTitleAsTheIssueWritesIt() {
        assertThat(openIssue().issue().title()).isEqualTo(title());
    }
}
