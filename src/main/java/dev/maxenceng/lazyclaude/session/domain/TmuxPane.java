package dev.maxenceng.lazyclaude.session.domain;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A tmux pane as {@code TMUX_PANE} names it: {@code %} and a number. */
public record TmuxPane(String value) {

    static final Pattern SHAPE = Pattern.compile("%\\d+");

    public TmuxPane {
        Assert.matchesPattern("tmux pane", value, SHAPE, "% and a number");
    }
}
