package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The line, or range of lines, of the diff the thread was written on, as of the commit it was written against. */
public record OriginallyPlaced(PlacedThread place) implements OriginalPlace {

    public OriginallyPlaced {
        Assert.notNull("place", place);
    }
}
