package dev.maxenceng.lazyclaude.board.domain;

/** The branch's changes are not known: not read, as for a ticket not in progress, or unreadable, as for a branch not created yet. */
public record UnreadChanges() implements BranchChanges {

    @Override
    public boolean touchesFrontend() {
        return false;
    }
}
