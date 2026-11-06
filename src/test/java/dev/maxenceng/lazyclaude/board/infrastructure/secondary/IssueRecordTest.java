package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.domain.ClosedAtFixture.closedAt;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.closedIssue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issue;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueNumbered;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueTitled;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.issueWithBody;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.labelsOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssueRecordFixture.openIssue;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(new IssueRecord(1, "t", "b", List.of(), "CLOSED", null, "2026-10-01T09:00:00Z").issue().state())
                .isEqualTo(ClosedIssue.builder().reason(new ClosedReason("")).closedAt(closedAt()).build());
    }

    @Test
    void shouldNameClosedAtAsMissingFromAClosedIssueThatLacksIt() {
        assertThat(new IssueRecord(1, "t", "b", List.of(), "CLOSED", "COMPLETED", null).missingFields().listed()).isEqualTo("closedAt");
    }

    @Test
    void shouldNotAskAnOpenIssueForClosedAt() {
        assertThat(new IssueRecord(1, "t", "b", List.of(), "OPEN", "", null).missingFields().any()).isFalse();
    }

    @Test
    void shouldRefuseAClosedAtThatIsNotATime() {
        assertThatThrownBy(() -> new IssueRecord(1, "t", "b", List.of(), "CLOSED", "COMPLETED", "yesterday").issue())
                .isInstanceOf(BacklogUnavailableException.class)
                .hasMessage("gh issue list wrote a closedAt that is not a time: yesterday");
    }

    @Test
    void shouldReadTheLabelsInTheOrderGitHubListsThem() {
        assertThat(openIssue("status:todo", "help wanted").issue().labels())
                .containsExactly(new LabelName("status:todo"), new LabelName("help wanted"));
    }

    @Test
    void shouldDropALabelWrittenBlank() {
        assertThat(new IssueRecord(1, "t", "b", labelsOf(" ", "bug"), "OPEN", "", null).issue().labels()).containsExactly(new LabelName("bug"));
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
        assertThat(new IssueRecord(1, "t", "b", null, "OPEN", "", null).issue().labels()).isEmpty();
    }

    @Test
    void shouldNameTheFieldsAnIssueLacks() {
        assertThat(new IssueRecord(null, " ", "b", List.of(), null, "", null).missingFields().listed()).isEqualTo("number, title, state");
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
