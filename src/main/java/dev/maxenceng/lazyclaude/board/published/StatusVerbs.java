package dev.maxenceng.lazyclaude.board.published;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The verbs a status offers, in the order the picker lists them; none for a status left no verb. */
public record StatusVerbs(Status status, List<Verb> verbs) {

    public StatusVerbs {
        Assert.notNull("status", status);
        Assert.notNull("verbs", verbs);
    }

    public static StatusVerbsBuilder builder() {
        return new StatusVerbsBuilder();
    }

    public static final class StatusVerbsBuilder {
        private Status status;
        private List<Verb> verbs;

        private StatusVerbsBuilder() {}

        public StatusVerbsBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public StatusVerbsBuilder verbs(List<Verb> verbs) {
            this.verbs = verbs;
            return this;
        }

        public StatusVerbs build() {
            return new StatusVerbs(status, verbs);
        }
    }
}
