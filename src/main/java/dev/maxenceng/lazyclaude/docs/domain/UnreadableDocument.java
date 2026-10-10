package dev.maxenceng.lazyclaude.docs.domain;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A document whose file could not be read; it stays listed, with the reason. */
public record UnreadableDocument(DocumentPath path, Reason reason) implements Document {

    public UnreadableDocument {
        Assert.notNull("path", path);
        Assert.notNull("reason", reason);
    }

    public static UnreadableDocumentBuilder builder() {
        return new UnreadableDocumentBuilder();
    }

    public static final class UnreadableDocumentBuilder {
        private DocumentPath path;
        private Reason reason;

        private UnreadableDocumentBuilder() {}

        public UnreadableDocumentBuilder path(DocumentPath path) {
            this.path = path;
            return this;
        }

        public UnreadableDocumentBuilder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public UnreadableDocument build() {
            return new UnreadableDocument(path, reason);
        }
    }
}
