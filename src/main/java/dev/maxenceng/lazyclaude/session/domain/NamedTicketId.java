package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket id as a transcript spelt it, in whatever case, before the board says whether it has such a ticket. */
public record NamedTicketId(String value) {

    /** What a word must look like to be read as a ticket id: the board's own shape, so every id it can hold can be named. */
    private static final Pattern SHAPE = Pattern.compile("[A-Za-z][A-Za-z0-9]*-\\d{1,18}");

    private static final Pattern PREFIX = Pattern.compile("^(" + SHAPE.pattern() + ")(?!\\d)");

    public NamedTicketId {
        Assert.notBlank("named ticket id", value);
    }

    /** {@code word} as a ticket id, if it has the shape of one. */
    public static Optional<NamedTicketId> fromWord(String word) {
        return SHAPE.matcher(word).matches() ? Optional.of(new NamedTicketId(word)) : Optional.empty();
    }

    /** The ticket id {@code branch} starts with, if it starts with one. */
    public static Optional<NamedTicketId> prefixOf(String branch) {
        Matcher prefix = PREFIX.matcher(branch);
        return prefix.find() ? Optional.of(new NamedTicketId(prefix.group(1))) : Optional.empty();
    }
}
