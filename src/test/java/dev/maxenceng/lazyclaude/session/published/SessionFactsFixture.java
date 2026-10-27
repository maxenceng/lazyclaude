package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;

public final class SessionFactsFixture {

    private SessionFactsFixture() {}

    public static ReadFacts readFacts() {
        return new ReadFacts(aiTitle(), firstPrompt());
    }

    public static ReadFacts titledFacts(SessionTitle title) {
        OpeningPrompt opening = title instanceof FirstPrompt prompt ? prompt : new NoFirstPrompt();
        return new ReadFacts(title, opening);
    }

    public static ReadFacts untitledFacts() {
        return new ReadFacts(new NoTitle(), new NoFirstPrompt());
    }

    public static UnreadableFacts unreadableFacts() {
        return new UnreadableFacts(reason());
    }
}
