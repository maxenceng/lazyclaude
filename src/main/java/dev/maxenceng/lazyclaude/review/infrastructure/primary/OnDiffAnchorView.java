package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingOnDiff;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;

/** A line of one side of a file, or a range of lines inside one hunk. */
public record OnDiffAnchorView(DiffPath path, PlacedThread position) implements PendingAnchorView, CommentAnchorView {

    static OnDiffAnchorView from(PendingOnDiff onDiff) {
        return builder().path(onDiff.path()).position(onDiff.position()).build();
    }

    @Override
    public PendingOnDiff toDomain() {
        return PendingOnDiff.builder().path(path).position(position).build();
    }

    public static OnDiffAnchorViewBuilder builder() {
        return new OnDiffAnchorViewBuilder();
    }

    public static final class OnDiffAnchorViewBuilder {
        private DiffPath path;
        private PlacedThread position;

        private OnDiffAnchorViewBuilder() {}

        public OnDiffAnchorViewBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public OnDiffAnchorViewBuilder position(PlacedThread position) {
            this.position = position;
            return this;
        }

        public OnDiffAnchorView build() {
            return new OnDiffAnchorView(path, position);
        }
    }
}
