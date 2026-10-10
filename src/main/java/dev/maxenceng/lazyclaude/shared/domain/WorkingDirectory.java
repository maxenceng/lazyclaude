package dev.maxenceng.lazyclaude.shared.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory lazyclaude was started in, or a session's {@code cwd}, absolute and normalised; the project is located from it. */
public record WorkingDirectory(Path path) {

    public WorkingDirectory {
        Assert.notNull("working directory", path);
        path = path.toAbsolutePath().normalize();
    }
}
