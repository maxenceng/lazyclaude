package dev.maxenceng.lazyclaude.board.domain;

/** The branch's changes could not be read, as when it does not exist yet. */
public record UnreadChanges() implements BranchChanges {

    @Override
    public boolean touchesFrontend() {
        return false;
    }
}
