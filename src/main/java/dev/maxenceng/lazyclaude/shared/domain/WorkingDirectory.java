package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory lazyclaude was started in, or a session's {@code cwd}, absolute and normalised; the project is located from it. */
public record WorkingDirectory(Path path) {

    public WorkingDirectory {
        Assert.notNull("working directory", path);
        path = path.toAbsolutePath().normalize();
    }

    /** Whether {@code other} is this directory or lies below it; compared by path segments, so a sibling sharing a name prefix is not. */
    public boolean contains(WorkingDirectory other) {
        return other.path().startsWith(path);
    }
}
