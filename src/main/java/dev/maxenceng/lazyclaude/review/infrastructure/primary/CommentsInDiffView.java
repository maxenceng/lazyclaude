package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.CommentsInDiff;

/**
 * A pull request's comments as the review screen shows them: every thread and every pending comment with its line, in diff order; the
 * threads that show and where; what Comments lists, pending comments first; and per place, the pending comment written last.
 */
public record CommentsInDiffView(
        List<ThreadInDiffView> threads,
        List<PendingInDiffView> pending,
        List<ThreadInDiffView> shown,
        List<CommentInDiffView> listed,
        List<PendingInDiffView> lastAtPlaces) {

    static CommentsInDiffView from(CommentsInDiff comments, boolean resolvedShown) {
        return builder()
                .threads(comments.threads().threads().stream().map(ThreadInDiffView::from).toList())
                .pending(comments.pending().comments().stream().map(PendingInDiffView::from).toList())
                .shown(comments.shown(resolvedShown).stream().map(ThreadInDiffView::from).toList())
                .listed(comments.listed().stream().map(CommentInDiffView::from).toList())
                .lastAtPlaces(comments.lastAtPlaces().stream().map(PendingInDiffView::from).toList())
                .build();
    }

    public static CommentsInDiffViewBuilder builder() {
        return new CommentsInDiffViewBuilder();
    }

    public static final class CommentsInDiffViewBuilder {
        private List<ThreadInDiffView> threads;
        private List<PendingInDiffView> pending;
        private List<ThreadInDiffView> shown;
        private List<CommentInDiffView> listed;
        private List<PendingInDiffView> lastAtPlaces;

        private CommentsInDiffViewBuilder() {}

        public CommentsInDiffViewBuilder threads(List<ThreadInDiffView> threads) {
            this.threads = threads;
            return this;
        }

        public CommentsInDiffViewBuilder pending(List<PendingInDiffView> pending) {
            this.pending = pending;
            return this;
        }

        public CommentsInDiffViewBuilder shown(List<ThreadInDiffView> shown) {
            this.shown = shown;
            return this;
        }

        public CommentsInDiffViewBuilder listed(List<CommentInDiffView> listed) {
            this.listed = listed;
            return this;
        }

        public CommentsInDiffViewBuilder lastAtPlaces(List<PendingInDiffView> lastAtPlaces) {
            this.lastAtPlaces = lastAtPlaces;
            return this;
        }

        public CommentsInDiffView build() {
            return new CommentsInDiffView(threads, pending, shown, listed, lastAtPlaces);
        }
    }
}
