package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A thread on a line of one side, or on the range from {@code first} to {@code last}; it shows under {@code last}. */
public record PlacedThread(ThreadSide side, DiffLineNumber first, DiffLineNumber last) implements ThreadPosition {

    public PlacedThread {
        Assert.notNull("side", side);
        Assert.notNull("first", first);
        Assert.notNull("last", last);
        Assert.notAfter("first", first.value(), last.value());
    }

    public static PlacedThreadBuilder builder() {
        return new PlacedThreadBuilder();
    }

    public static final class PlacedThreadBuilder {
        private ThreadSide side;
        private DiffLineNumber first;
        private DiffLineNumber last;

        private PlacedThreadBuilder() {}

        public PlacedThreadBuilder side(ThreadSide side) {
            this.side = side;
            return this;
        }

        public PlacedThreadBuilder first(DiffLineNumber first) {
            this.first = first;
            return this;
        }

        public PlacedThreadBuilder last(DiffLineNumber last) {
            this.last = last;
            return this;
        }

        public PlacedThread build() {
            return new PlacedThread(side, first, last);
        }
    }
}
