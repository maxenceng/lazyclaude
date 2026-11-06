package dev.maxenceng.lazyclaude.session.published;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The Claude Code plugin whose {@code ticket} skill a ticket's new session runs. */
public record TicketPlugin(String value) {

    /** A plugin name as Claude Code installs one: no space, colon or slash, which would split {@code /<plugin>:ticket}. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private static final String NAME_SHAPE = "a plugin name such as ddd-workflow";

    public TicketPlugin {
        Assert.notBlank("ticket plugin", value);
        Assert.matchesPattern("ticket plugin", value, NAME, NAME_SHAPE);
    }

    /** Whether {@code text} can name a plugin, for a reader that refuses in its own words. */
    public static boolean isName(String text) {
        return NAME.matcher(text).matches();
    }
}
