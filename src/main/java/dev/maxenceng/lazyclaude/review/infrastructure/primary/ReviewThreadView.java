package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** A conversation on the diff: where it sits, whether it is resolved or still open, and its comments, first to last. */
public record ReviewThreadView(DiffPath path, ThreadPosition position, boolean resolved, boolean open, List<ThreadComment> comments) {



    static ReviewThreadView from(ReviewThread thread) {
        return builder()
                .path(thread.path())
                .position(thread.position())
                .resolved(thread.resolved())
                .open(thread.open())
                .comments(thread.comments())
                .build();
    }

    ReviewThread toDomain() {
        return ReviewThread.builder()
                .path(path)
                .position(position)
                .resolved(resolved)
                .comments(comments)
                .build();
    }

    public static ReviewThreadViewBuilder builder() {
        return new ReviewThreadViewBuilder();
    }

    public static final class ReviewThreadViewBuilder {
        private DiffPath path;
        private ThreadPosition position;
        private boolean resolved;
        private boolean open;
        private List<ThreadComment> comments;

        private ReviewThreadViewBuilder() {}

        public ReviewThreadViewBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public ReviewThreadViewBuilder position(ThreadPosition position) {
            this.position = position;
            return this;
        }

        public ReviewThreadViewBuilder resolved(boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public ReviewThreadViewBuilder open(boolean open) {
            this.open = open;
            return this;
        }

        public ReviewThreadViewBuilder comments(List<ThreadComment> comments) {
            this.comments = comments;
            return this;
        }

        public ReviewThreadView build() {
            return new ReviewThreadView(path, position, resolved, open, comments);
        }
    }
}
