package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.shared.domain.BranchNameFixture.branchName;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class TicketWorktreeFixture {

    private TicketWorktreeFixture() {}

    public static TicketWorktree ticketWorktree() {
        return TicketWorktree.builder().project(project()).branch(branchName()).build();
    }

    public static TicketWorktree nowhereTicketWorktree() {
        return TicketWorktree.builder().project(new Project(Path.of("/"))).branch(branchName()).build();
    }

    public static TicketStart ticketStart(Verb verb) {
        return TicketStart.builder().worktree(ticketWorktree()).ticket(linkedTicketId()).verb(verb).build();
    }
}
