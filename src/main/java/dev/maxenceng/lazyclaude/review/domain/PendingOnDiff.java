package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;

/** A pending comment on a line of one side of a file, or on a range of lines inside one hunk. */
public record PendingOnDiff(DiffPath path, PlacedThread position) implements PendingAnchor {

    public PendingOnDiff {
        Assert.notNull("path", path);
        Assert.notNull("position", position);
    }

    public static PendingOnDiffBuilder builder() {
        return new PendingOnDiffBuilder();
    }

    public static final class PendingOnDiffBuilder {
        private DiffPath path;
        private PlacedThread position;

        private PendingOnDiffBuilder() {}

        public PendingOnDiffBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public PendingOnDiffBuilder position(PlacedThread position) {
            this.position = position;
            return this;
        }

        public PendingOnDiff build() {
            return new PendingOnDiff(path, position);
        }
    }
}
