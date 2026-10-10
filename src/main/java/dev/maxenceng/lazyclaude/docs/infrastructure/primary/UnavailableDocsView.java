package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import dev.maxenceng.lazyclaude.docs.domain.UnavailableDocs;
import dev.maxenceng.lazyclaude.docs.published.Reason;

/** A {@code docs/} that cannot be listed, and why. */
public record UnavailableDocsView(Reason reason) implements DocsView {

    static UnavailableDocsView from(UnavailableDocs docs) {
        return builder().reason(docs.reason()).build();
    }

    public static UnavailableDocsViewBuilder builder() {
        return new UnavailableDocsViewBuilder();
    }

    public static final class UnavailableDocsViewBuilder {
        private Reason reason;

        private UnavailableDocsViewBuilder() {}

        public UnavailableDocsViewBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnavailableDocsView build() {
            return new UnavailableDocsView(reason);
        }
    }
}
