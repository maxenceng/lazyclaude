package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.UnavailableDiff;
import dev.maxenceng.lazyclaude.review.published.Reason;

/** The diff could not be read, and why; too large means GitHub itself refused to produce it. */
public record UnavailableDiffView(Reason reason, boolean tooLarge) implements PullRequestDiffView {



    static UnavailableDiffView from(UnavailableDiff unavailable) {
        return builder().reason(unavailable.reason()).tooLarge(unavailable.tooLarge()).build();
    }

    public static UnavailableDiffViewBuilder builder() {
        return new UnavailableDiffViewBuilder();
    }

    public static final class UnavailableDiffViewBuilder {
        private Reason reason;
        private boolean tooLarge;

        private UnavailableDiffViewBuilder() {}

        public UnavailableDiffViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableDiffViewBuilder tooLarge(boolean tooLarge) {
            this.tooLarge = tooLarge;
            return this;
        }

        public UnavailableDiffView build() {
            return new UnavailableDiffView(reason, tooLarge);
        }
    }
}
