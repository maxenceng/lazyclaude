package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;

public final class IssueRecordFixture {

    private IssueRecordFixture() {}

    static IssueRecord openIssue(String... labels) {
        return issue("OPEN", "", labels);
    }

    static IssueRecord closedIssue(String reason, String... labels) {
        return issue("CLOSED", reason, labels);
    }

    static IssueRecord issue(String state, String reason, String... labels) {
        return new IssueRecord(1, "A board from the backlog", "Body.", labelsOf(labels), state, reason);
    }

    static IssueRecord issueTitled(String title) {
        return new IssueRecord(1, title, "Body.", List.of(), "OPEN", "");
    }

    static IssueRecord issueWithBody(String body) {
        return new IssueRecord(1, "A board from the backlog", body, List.of(), "OPEN", "");
    }

    static IssueRecord issueNumbered(Integer number, String title, String state) {
        return new IssueRecord(number, title, "Body.", List.of(), state, "");
    }

    static String[] todoLabels() {
        return new String[] {"status:todo", "context:board", "type:feature"};
    }

    static List<LabelRecord> labelsOf(String... names) {
        return Arrays.stream(names).map(LabelRecord::new).toList();
    }

    static BacklogEntry unreadable(String why) {
        return UnreadableTicket.builder().id(ticketId()).reason(new Reason(why)).build();
    }

    static TicketBranch branchOf(IssueRecord issue) {
        return ((Ticket) issue.entry()).branch();
    }

    static TicketBody bodyOf(IssueRecord issue) {
        return ((Ticket) issue.entry()).body();
    }
}
