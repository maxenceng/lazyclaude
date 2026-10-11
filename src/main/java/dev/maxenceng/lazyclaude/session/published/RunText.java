package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The characters of one run, a space for each empty cell; a double-width character is followed by nothing. */
public record RunText(String value) {

    public RunText {
        Assert.notNull("run text", value);
    }
}
