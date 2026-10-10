package dev.maxenceng.lazyclaude.session.published;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The directory a session runs in, its {@code cwd}. */
public record SessionDirectory(Path value) {

    public SessionDirectory {
        Assert.notNull("session directory", value);
    }
}
