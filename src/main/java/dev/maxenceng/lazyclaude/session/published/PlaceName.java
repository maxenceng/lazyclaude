package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What a group of sessions elsewhere is called: its project's name, or the directory when the sessions are outside git. */
public record PlaceName(String value) {

    public PlaceName {
        Assert.notBlank("place name", value);
    }
}
