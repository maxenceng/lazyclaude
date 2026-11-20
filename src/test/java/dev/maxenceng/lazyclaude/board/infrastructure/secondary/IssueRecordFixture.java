package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Arrays;

public final class IssueRecordFixture {

    private IssueRecordFixture() {}

    static IssueRecord openIssue(String... labels) {
        return issue("OPEN", "", labels);
    }

    static IssueRecord closedIssue(String reason, String... labels) {
        return issue("CLOSED", reason, labels);
    }

    static IssueRecord issue(String state, String reason, String... labels) {
        return new IssueRecord(1, "A board from the backlog", "Body.", labelsOf(labels), state, reason, closedAtOf(state), noLinkedBranches());
    }

    static IssueRecord issueTitled(String title) {
        return new IssueRecord(1, title, "Body.", labelsOf(), "OPEN", "", null, noLinkedBranches());
    }

    static IssueRecord issueTitledLinkedTo(String title, String... branches) {
        return new IssueRecord(1, title, "Body.", labelsOf(), "OPEN", "", null, linkedTo(branches));
    }

    static IssueRecord issueLinkedTo(NodesRecord<LinkedBranchRecord> linkedBranches) {
        return new IssueRecord(1, "A board from the backlog", "Body.", labelsOf(), "OPEN", "", null, linkedBranches);
    }

    static IssueRecord issueWithBody(String body) {
        return new IssueRecord(1, "A board from the backlog", body, labelsOf(), "OPEN", "", null, noLinkedBranches());
    }

    static IssueRecord issueNumbered(Integer number, String title, String state) {
        return new IssueRecord(number, title, "Body.", labelsOf(), state, "", closedAtOf(state), noLinkedBranches());
    }

    static IssueRecord issueWith(Integer number, String title, NodesRecord<LabelRecord> labels, String state, String reason, String closedAt) {
        return new IssueRecord(number, title, "b", labels, state, reason, closedAt, noLinkedBranches());
    }

    static String closedAtOf(String state) {
        return "CLOSED".equals(state) ? "2026-10-01T09:00:00Z" : null;
    }

    static NodesRecord<LabelRecord> labelsOf(String... names) {
        return new NodesRecord<>(Arrays.stream(names).map(LabelRecord::new).toList());
    }

    static NodesRecord<LinkedBranchRecord> noLinkedBranches() {
        return linkedTo();
    }

    static NodesRecord<LinkedBranchRecord> linkedTo(String... branches) {
        return new NodesRecord<>(Arrays.stream(branches).map(RefRecord::new).map(LinkedBranchRecord::new).toList());
    }

    static NodesRecord<LinkedBranchRecord> linkedBranchesAbsent() {
        return new NodesRecord<>(null);
    }

}
