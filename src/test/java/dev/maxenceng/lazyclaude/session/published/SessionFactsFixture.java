package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;

public final class SessionFactsFixture {

    private SessionFactsFixture() {}

    public static ReadFacts readFacts() {
        return ReadFacts.builder().title(aiTitle()).opening(firstPrompt()).build();
    }

    public static ReadFacts titledFacts(SessionTitle title) {
        OpeningPrompt opening = title instanceof FirstPrompt prompt ? prompt : new NoFirstPrompt();
        return ReadFacts.builder().title(title).opening(opening).build();
    }

    public static ReadFacts untitledFacts() {
        return ReadFacts.builder().title(new NoTitle()).opening(new NoFirstPrompt()).build();
    }

    public static UnreadableFacts unreadableFacts() {
        return new UnreadableFacts(reason());
    }
}
