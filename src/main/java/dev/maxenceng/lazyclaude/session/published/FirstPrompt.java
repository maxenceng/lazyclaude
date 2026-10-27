package dev.maxenceng.lazyclaude.session.published;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What the session was first asked, cut to one line: every run of whitespace, line breaks included, becomes one space. */
public record FirstPrompt(String value) implements SessionTitle, OpeningPrompt {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String SPACE = " ";

    public FirstPrompt {
        Assert.notBlank("first prompt", value);
        value = WHITESPACE.matcher(value.strip()).replaceAll(SPACE);
    }
}
