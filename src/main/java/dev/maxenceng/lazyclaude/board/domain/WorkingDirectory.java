package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory lazyclaude was started in, absolute and normalised; the project is located from it. */
public record WorkingDirectory(Path path) {

    public WorkingDirectory {
        Assert.notNull("working directory", path);
        path = path.toAbsolutePath().normalize();
    }
}
