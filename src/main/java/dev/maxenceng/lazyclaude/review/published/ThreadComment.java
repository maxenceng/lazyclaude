package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One comment of a thread: who wrote it and what it says. */
public record ThreadComment(CommentAuthor author, CommentBody body) {

    public ThreadComment {
        Assert.notNull("author", author);
        Assert.notNull("body", body);
    }

    public static ThreadCommentBuilder builder() {
        return new ThreadCommentBuilder();
    }

    public static final class ThreadCommentBuilder {
        private CommentAuthor author;
        private CommentBody body;

        private ThreadCommentBuilder() {}

        public ThreadCommentBuilder author(CommentAuthor author) {
            this.author = author;
            return this;
        }

        public ThreadCommentBuilder body(CommentBody body) {
            this.body = body;
            return this;
        }

        public ThreadComment build() {
            return new ThreadComment(author, body);
        }
    }
}
