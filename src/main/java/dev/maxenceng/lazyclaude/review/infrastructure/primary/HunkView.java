package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.Hunk;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;

/** One {@code @@} section of a file's diff. */
public record HunkView(HunkHeader header, List<DiffLine> lines) {



    static HunkView from(Hunk hunk) {
        return builder().header(hunk.header()).lines(hunk.lines()).build();
    }

    Hunk toDomain() {
        return Hunk.builder().header(header).lines(lines).build();
    }

    public static HunkViewBuilder builder() {
        return new HunkViewBuilder();
    }

    public static final class HunkViewBuilder {
        private HunkHeader header;
        private List<DiffLine> lines;

        private HunkViewBuilder() {}

        public HunkViewBuilder header(HunkHeader header) {
            this.header = header;
            return this;
        }

        public HunkViewBuilder lines(List<DiffLine> lines) {
            this.lines = lines;
            return this;
        }

        public HunkView build() {
            return new HunkView(header, lines);
        }
    }
}
