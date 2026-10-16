package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A file a branch changes, by its path from the repository root. */
public record ChangedFile(Path path) {

    private static final Path FRONTEND = Path.of("frontend");

    public ChangedFile {
        Assert.notNull("changed file", path);
    }

    boolean underFrontend() {
        return path.startsWith(FRONTEND);
    }
}
