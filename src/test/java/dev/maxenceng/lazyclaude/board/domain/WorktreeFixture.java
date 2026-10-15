package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.checkedOut;
import static dev.maxenceng.lazyclaude.board.domain.CheckedOutBranchFixture.trunk;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.domain.Worktree.WorktreeBuilder;
import dev.maxenceng.lazyclaude.board.published.TicketBranch;

public final class WorktreeFixture {

    private WorktreeFixture() {}

    public static WorktreeBuilder worktreeBuilder() {
        return Worktree.builder().path(new WorktreePath(Path.of("/work/elsewhere"))).head(checkedOut(ticketBranch()));
    }

    public static Worktree worktreeOn(CheckedOutBranch head) {
        return worktreeBuilder().head(head).build();
    }

    public static Worktree rootCheckoutOn(CheckedOutBranch head) {
        return worktreeBuilder().path(new WorktreePath(Path.of("/work/app"))).head(head).build();
    }

    public static Worktree ticketWorktreeOf(TicketBranch pathBranch, CheckedOutBranch head) {
        return worktreeBuilder()
                .path(new WorktreePath(Path.of("/work/app.worktrees").resolve(pathBranch.value())))
                .head(head)
                .build();
    }

    public static Worktree trunkTicketWorktree() {
        return ticketWorktreeOf(ticketBranch(), trunk());
    }

    public static Worktree detachedTicketWorktree() {
        return worktreeBuilder()
                .path(new WorktreePath(Path.of("/work/app.worktrees").resolve(ticketBranch().value())))
                .head(new DetachedHead())
                .build();
    }
}
