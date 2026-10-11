package dev.maxenceng.lazyclaude.session.domain;

import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A ticket id as a transcript spelt it, in whatever case, before the board says whether it has such a ticket. */
public record NamedTicketId(String value) {

    /** What a word must look like to be read as a ticket id: the board's own shape, so every id it can hold can be named. */
    public static final Pattern SHAPE = Pattern.compile("[A-Za-z][A-Za-z0-9]*-\\d{1,18}");

    public NamedTicketId {
        Assert.notBlank("named ticket id", value);
    }
}
