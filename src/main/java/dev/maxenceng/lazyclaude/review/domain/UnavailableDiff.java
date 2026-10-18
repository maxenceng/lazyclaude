package dev.maxenceng.lazyclaude.review.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** The diff could not be read, and why; too large means GitHub itself refused to produce it. */
public record UnavailableDiff(Reason reason, boolean tooLarge) implements DiffReading {

    public UnavailableDiff {
        Assert.notNull("reason", reason);
    }

    public static UnavailableDiffBuilder builder() {
        return new UnavailableDiffBuilder();
    }

    public static final class UnavailableDiffBuilder {
        private Reason reason;
        private Boolean tooLarge;

        private UnavailableDiffBuilder() {}

        public UnavailableDiffBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableDiffBuilder tooLarge(boolean tooLarge) {
            this.tooLarge = tooLarge;
            return this;
        }

        public UnavailableDiff build() {
            Assert.notNull("tooLarge", tooLarge);
            return new UnavailableDiff(reason, tooLarge);
        }
    }
}
