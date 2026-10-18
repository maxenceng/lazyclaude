package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineIndex;

/** A hunk with the file it belongs to and the place of its first line in the diff's one scroll. */
record HunkInDiff(DiffFile file, Hunk hunk, DiffLineIndex start) {

    HunkInDiff {
        Assert.notNull("file", file);
        Assert.notNull("hunk", hunk);
        Assert.notNull("start", start);
    }

    /** Whether the diff line at {@code index} is one of this hunk's. */
    boolean holds(int index) {
        return index >= start.value() && index < start.value() + hunk.lines().size();
    }

    /** The lines from the one at {@code from} to the one at {@code to}, both of them this hunk's. */
    List<DiffLine> linesFrom(int from, int to) {
        return hunk.lines().subList(from - start.value(), to - start.value() + 1);
    }

    static HunkInDiffBuilder builder() {
        return new HunkInDiffBuilder();
    }

    static final class HunkInDiffBuilder {
        private DiffFile file;
        private Hunk hunk;
        private DiffLineIndex start;

        private HunkInDiffBuilder() {}

        HunkInDiffBuilder file(DiffFile file) {
            this.file = file;
            return this;
        }

        HunkInDiffBuilder hunk(Hunk hunk) {
            this.hunk = hunk;
            return this;
        }

        HunkInDiffBuilder start(DiffLineIndex start) {
            this.start = start;
            return this;
        }

        HunkInDiff build() {
            return new HunkInDiff(file, hunk, start);
        }
    }
}
