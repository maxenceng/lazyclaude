package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;

/** One {@code @@} section of a file's diff. */
public record Hunk(HunkHeader header, List<DiffLine> lines) {

    public Hunk {
        Assert.notNull("header", header);
        Assert.notNull("lines", lines);
    }

    public static HunkBuilder builder() {
        return new HunkBuilder();
    }

    public static final class HunkBuilder {
        private HunkHeader header;
        private List<DiffLine> lines;

        private HunkBuilder() {}

        public HunkBuilder header(HunkHeader header) {
            this.header = header;
            return this;
        }

        public HunkBuilder lines(List<DiffLine> lines) {
            this.lines = lines;
            return this;
        }

        public Hunk build() {
            return new Hunk(header, lines);
        }
    }
}
