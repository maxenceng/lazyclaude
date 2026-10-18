package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.PendingReply;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** A thread the comment answers, which sits where {@code position} says in the file at {@code path}. */
public record ReplyAnchorView(ThreadId thread, DiffPath path, ThreadPosition position) implements PendingAnchorView {

    static ReplyAnchorView from(PendingReply reply) {
        return builder().thread(reply.thread()).path(reply.path()).position(reply.position()).build();
    }

    @Override
    public PendingReply toDomain() {
        return PendingReply.builder().thread(thread).path(path).position(position).build();
    }

    public static ReplyAnchorViewBuilder builder() {
        return new ReplyAnchorViewBuilder();
    }

    public static final class ReplyAnchorViewBuilder {
        private ThreadId thread;
        private DiffPath path;
        private ThreadPosition position;

        private ReplyAnchorViewBuilder() {}

        public ReplyAnchorViewBuilder thread(ThreadId thread) {
            this.thread = thread;
            return this;
        }

        public ReplyAnchorViewBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public ReplyAnchorViewBuilder position(ThreadPosition position) {
            this.position = position;
            return this;
        }

        public ReplyAnchorView build() {
            return new ReplyAnchorView(thread, path, position);
        }
    }
}
