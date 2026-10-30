package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.published.BodyFixture.body;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.board.published.TitleFixture.title;

import java.util.Arrays;

import dev.maxenceng.lazyclaude.board.domain.GitHubIssue.GitHubIssueBuilder;
import dev.maxenceng.lazyclaude.board.published.LabelName;
import dev.maxenceng.lazyclaude.board.published.Reason;

public final class GitHubIssueFixture {

    private GitHubIssueFixture() {}

    public static GitHubIssueBuilder gitHubIssueBuilder() {
        return GitHubIssue.builder().id(ticketId()).branch(ticketBranch()).title(title()).body(body()).state(new OpenIssue());
    }

    public static GitHubIssue openIssue(String... labels) {
        return gitHubIssueBuilder().labels(Arrays.stream(labels).map(LabelName::new).toList()).build();
    }

    public static GitHubIssue closedIssue(String reason, String... labels) {
        return gitHubIssueBuilder()
                .state(new ClosedIssue(new ClosedReason(reason)))
                .labels(Arrays.stream(labels).map(LabelName::new).toList())
                .build();
    }

    public static String[] todoLabels() {
        return new String[] {"status:todo", "context:board", "type:feature"};
    }

    public static BacklogEntry unreadable(String why) {
        return UnreadableTicket.builder().id(ticketId()).reason(new Reason(why)).build();
    }
}
