package dev.maxenceng.lazyclaude.session.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A readable transcript's title and what it opened with, either of which it may lack. */
public record ReadFacts(SessionTitle title, OpeningPrompt opening) implements SessionFacts {

    public ReadFacts {
        Assert.notNull("title", title);
        Assert.notNull("opening prompt", opening);
    }

    public static ReadFactsBuilder builder() {
        return new ReadFactsBuilder();
    }

    public static final class ReadFactsBuilder {
        private SessionTitle title;
        private OpeningPrompt opening;

        private ReadFactsBuilder() {}

        public ReadFactsBuilder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        public ReadFactsBuilder opening(OpeningPrompt opening) {
            this.opening = opening;
            return this;
        }

        public ReadFacts build() {
            return new ReadFacts(title, opening);
        }
    }
}
