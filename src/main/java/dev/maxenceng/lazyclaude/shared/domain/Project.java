package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;
import java.util.Objects;

/** The directory lazyclaude works on: the root of the main worktree, as {@code ProjectLocator} finds it. */
public record Project(Path root) {

    public Project {
        Objects.requireNonNull(root, "root");
        if (!root.isAbsolute()) {
            throw new IllegalArgumentException("a project root is absolute, got " + root);
        }
    }
}
