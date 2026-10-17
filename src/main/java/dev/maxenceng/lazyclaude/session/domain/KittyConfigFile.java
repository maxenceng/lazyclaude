package dev.maxenceng.lazyclaude.session.domain;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A file of kitty's configuration: {@code kitty.conf} itself, or one it includes. */
public record KittyConfigFile(Path path) {

    public KittyConfigFile {
        Assert.absolute("kitty config file", path);
    }
}
