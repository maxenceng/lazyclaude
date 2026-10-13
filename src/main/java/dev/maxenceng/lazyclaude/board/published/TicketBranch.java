package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The branch a ticket's work happens on: its file's name without {@code .md}. */
public record TicketBranch(String value) {

    private static final String MARKDOWN = ".md";

    public TicketBranch {
        Assert.notBlank("ticket branch", value);
    }

    public static TicketBranch of(FileName file) {
        String name = file.value();
        return new TicketBranch(name.endsWith(MARKDOWN) ? name.substring(0, name.length() - MARKDOWN.length()) : name);
    }
}
