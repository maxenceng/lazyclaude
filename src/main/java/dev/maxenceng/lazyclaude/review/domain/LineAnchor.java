package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadSide;

/** Where a thread hangs: a line number on one side of one file. */
record LineAnchor(DiffPath path, ThreadSide side, DiffLineNumber number) {

    static LineAnchorBuilder builder() {
        return new LineAnchorBuilder();
    }

    static final class LineAnchorBuilder {
        private DiffPath path;
        private ThreadSide side;
        private DiffLineNumber number;

        private LineAnchorBuilder() {}

        LineAnchorBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        LineAnchorBuilder side(ThreadSide side) {
            this.side = side;
            return this;
        }

        LineAnchorBuilder number(DiffLineNumber number) {
            this.number = number;
            return this;
        }

        LineAnchor build() {
            return new LineAnchor(path, side, number);
        }
    }
}
