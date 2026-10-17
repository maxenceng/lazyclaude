package dev.maxenceng.lazyclaude.session.domain;

/** No merged pull request is known for the ticket branch, so what the merge covered cannot be told and nothing is clear to remove. */
public record NoMergedPullRequest() implements WorktreeState {

    @Override
    public boolean clear() {
        return false;
    }
}
