package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A path in a worktree that {@code git status --porcelain} reports: modified, staged, deleted or untracked, as git spells it. */
public record DirtyPath(String value) {

    public DirtyPath {
        Assert.notBlank("dirty path", value);
    }
}
