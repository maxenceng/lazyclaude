package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.FileStatus;

/** One file of a pull request's diff: what was done to it, and its hunks. */
public record DiffFile(DiffPath path, FileStatus status, List<Hunk> hunks) {

    public DiffFile {
        Assert.notNull("path", path);
        Assert.notNull("status", status);
        Assert.notNull("hunks", hunks);
    }

    public static DiffFileBuilder builder() {
        return new DiffFileBuilder();
    }

    public static final class DiffFileBuilder {
        private DiffPath path;
        private FileStatus status;
        private List<Hunk> hunks;

        private DiffFileBuilder() {}

        public DiffFileBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public DiffFileBuilder status(FileStatus status) {
            this.status = status;
            return this;
        }

        public DiffFileBuilder hunks(List<Hunk> hunks) {
            this.hunks = hunks;
            return this;
        }

        public DiffFile build() {
            return new DiffFile(path, status, hunks);
        }
    }
}
