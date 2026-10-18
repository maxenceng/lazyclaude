package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingInDiff;
import dev.maxenceng.lazyclaude.review.published.ThreadLine;

/** A pending comment and the diff line it shows under. */
public record PendingInDiffView(PendingCommentView comment, ThreadLine line) {

    static PendingInDiffView from(PendingInDiff placed) {
        return builder().comment(PendingCommentView.from(placed.comment())).line(placed.line()).build();
    }

    public static PendingInDiffViewBuilder builder() {
        return new PendingInDiffViewBuilder();
    }

    public static final class PendingInDiffViewBuilder {
        private PendingCommentView comment;
        private ThreadLine line;

        private PendingInDiffViewBuilder() {}

        public PendingInDiffViewBuilder comment(PendingCommentView comment) {
            this.comment = comment;
            return this;
        }

        public PendingInDiffViewBuilder line(ThreadLine line) {
            this.line = line;
            return this;
        }

        public PendingInDiffView build() {
            return new PendingInDiffView(comment, line);
        }
    }
}
