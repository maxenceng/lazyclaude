package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The head is no longer the commit the pending review was written on; submitting still goes through. */
public record MovedHead(CommitSha writtenOn, CommitSha head) implements HeadMove {

    public MovedHead {
        Assert.notNull("written on", writtenOn);
        Assert.notNull("head", head);
    }

    public static MovedHeadBuilder builder() {
        return new MovedHeadBuilder();
    }

    public static final class MovedHeadBuilder {
        private CommitSha writtenOn;
        private CommitSha head;

        private MovedHeadBuilder() {}

        public MovedHeadBuilder writtenOn(CommitSha writtenOn) {
            this.writtenOn = writtenOn;
            return this;
        }

        public MovedHeadBuilder head(CommitSha head) {
            this.head = head;
            return this;
        }

        public MovedHead build() {
            return new MovedHead(writtenOn, head);
        }
    }
}
