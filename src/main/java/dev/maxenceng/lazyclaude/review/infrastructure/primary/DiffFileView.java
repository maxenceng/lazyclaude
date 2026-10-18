package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.DiffFile;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.FileStatus;

/** One file of the diff. */
public record DiffFileView(DiffPath path, FileStatus status, List<HunkView> hunks) {



    static DiffFileView from(DiffFile file) {
        return builder()
                .path(file.path())
                .status(file.status())
                .hunks(file.hunks().stream().map(HunkView::from).toList())
                .build();
    }

    public static DiffFileViewBuilder builder() {
        return new DiffFileViewBuilder();
    }

    public static final class DiffFileViewBuilder {
        private DiffPath path;
        private FileStatus status;
        private List<HunkView> hunks;

        private DiffFileViewBuilder() {}

        public DiffFileViewBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public DiffFileViewBuilder status(FileStatus status) {
            this.status = status;
            return this;
        }

        public DiffFileViewBuilder hunks(List<HunkView> hunks) {
            this.hunks = hunks;
            return this;
        }

        public DiffFileView build() {
            return new DiffFileView(path, status, hunks);
        }
    }
}
