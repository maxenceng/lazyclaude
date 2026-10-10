package dev.maxenceng.lazyclaude.board.published;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The root of the project a board was read from, absolute; the shell hands it to the other contexts. */
public record ProjectRoot(Path value) {

    public ProjectRoot {
        Assert.notNull("project root", value);
    }
}
