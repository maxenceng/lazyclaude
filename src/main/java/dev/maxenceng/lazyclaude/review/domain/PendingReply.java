package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.ThreadId;
import dev.maxenceng.lazyclaude.review.published.ThreadPosition;

/** A pending comment answering an existing thread, which sits where {@code position} says in the file at {@code path}. */
public record PendingReply(ThreadId thread, DiffPath path, ThreadPosition position) implements PendingAnchor {

    public PendingReply {
        Assert.notNull("thread", thread);
        Assert.notNull("path", path);
        Assert.notNull("position", position);
    }

    public static PendingReplyBuilder builder() {
        return new PendingReplyBuilder();
    }

    public static final class PendingReplyBuilder {
        private ThreadId thread;
        private DiffPath path;
        private ThreadPosition position;

        private PendingReplyBuilder() {}

        public PendingReplyBuilder thread(ThreadId thread) {
            this.thread = thread;
            return this;
        }

        public PendingReplyBuilder path(DiffPath path) {
            this.path = path;
            return this;
        }

        public PendingReplyBuilder position(ThreadPosition position) {
            this.position = position;
            return this;
        }

        public PendingReply build() {
            return new PendingReply(thread, path, position);
        }
    }
}
