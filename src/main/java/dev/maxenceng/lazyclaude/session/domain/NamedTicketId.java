package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket id as a transcript spelt it, an issue number with or without its {@code #}, before the board says whether it has such a ticket. */
public record NamedTicketId(String value) {

    /** What a word must look like to be read as a ticket id: the board's own shape, so every id it can hold can be named. */
    private static final Pattern SHAPE = Pattern.compile("#?([1-9]\\d{0,17})");

    public NamedTicketId {
        Assert.notBlank("named ticket id", value);
    }

    /** {@code word} as a ticket id, if it has the shape of one. */
    public static Optional<NamedTicketId> fromWord(String word) {
        Matcher shape = SHAPE.matcher(word);
        return shape.matches() ? Optional.of(new NamedTicketId(shape.group(1))) : Optional.empty();
    }
}
