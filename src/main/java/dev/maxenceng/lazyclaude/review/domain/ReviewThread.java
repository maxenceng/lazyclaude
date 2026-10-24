package dev.maxenceng.lazyclaude.review.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** A conversation on a pull request's diff: where it sits, whether it is resolved, and its comments, first to last. */
public record ReviewThread(ThreadId id, DiffPath path, ThreadPosition position, boolean resolved, List<ThreadComment> comments) {

    public ReviewThread {
        Assert.notNull("id", id);
        Assert.notNull("path", path);
        Assert.notNull("position", position);
        Assert.notNull("comments", comments);
    }

    /** Open means still to be answered: not resolved, and not outdated. */
    public boolean open() {
        return !resolved && position instanceof PlacedThread;
    }

    public static ReviewThreadBuilder builder() {
        return new ReviewThreadBuilder();
    }

    public static final class ReviewThreadBuilder {
        private ThreadId id;
        private DiffPath path;
        private ThreadPosition position;
        private boolean resolved;
        private List<ThreadComment> comments;

        private ReviewThreadBuilder() {}

        public ReviewThreadBuilder id(ThreadId id) {
            this.id = id;
            return this;
        }

        public ReviewThreadBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public ReviewThreadBuilder position(ThreadPosition position) {
            this.position = position;
            return this;
        }

        public ReviewThreadBuilder resolved(boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public ReviewThreadBuilder comments(List<ThreadComment> comments) {
            this.comments = comments;
            return this;
        }

        public ReviewThread build() {
            return new ReviewThread(id, path, position, resolved, comments);
        }
    }
}
