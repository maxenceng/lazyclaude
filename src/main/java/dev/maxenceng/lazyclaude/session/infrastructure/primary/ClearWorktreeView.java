package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

/** A ticket worktree with nothing in it that would be lost: no dirty path, no commit beyond the merge, no live session. */
public record ClearWorktreeView(Path shown) implements WorktreeShownView {}
