package dev.maxenceng.lazyclaude.session.domain;

/**
 * The sessions could not be read, so whether one runs in the ticket worktree cannot be told and nothing is clear to remove: a
 * {@code claude} started outside lazyclaude would lose its directory.
 */
public record NoSessionsRead() implements WorktreeState {

    @Override
    public boolean clear() {
        return false;
    }
}
