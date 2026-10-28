package dev.maxenceng.lazyclaude.session.domain;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.OpeningPrompt;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TranscriptReading;

/** What the transcript of a logged session says of it: its title, what it was first asked, whether it can be read now, when it was last active and its {@code /ticket} runs. */
public record TranscriptFacts(
        SessionTitle title, OpeningPrompt opening, TranscriptReading reading, LastActivity lastActivity, LinkFacts links) {

    public TranscriptFacts {
        Assert.notNull("title", title);
        Assert.notNull("opening prompt", opening);
        Assert.notNull("reading", reading);
        Assert.notNull("last activity", lastActivity);
        Assert.notNull("links", links);
    }

    public static TranscriptFactsBuilder builder() {
        return new TranscriptFactsBuilder();
    }

    public static final class TranscriptFactsBuilder {
        private SessionTitle title;
        private OpeningPrompt opening;
        private TranscriptReading reading;
        private LastActivity lastActivity;
        private LinkFacts links;

        private TranscriptFactsBuilder() {}

        public TranscriptFactsBuilder title(SessionTitle title) {
            this.title = title;
            return this;
        }

        public TranscriptFactsBuilder opening(OpeningPrompt opening) {
            this.opening = opening;
            return this;
        }

        public TranscriptFactsBuilder reading(TranscriptReading reading) {
            this.reading = reading;
            return this;
        }

        public TranscriptFactsBuilder lastActivity(LastActivity lastActivity) {
            this.lastActivity = lastActivity;
            return this;
        }

        public TranscriptFactsBuilder links(LinkFacts links) {
            this.links = links;
            return this;
        }

        public TranscriptFacts build() {
            return new TranscriptFacts(title, opening, reading, lastActivity, links);
        }
    }
}
