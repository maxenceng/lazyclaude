package dev.maxenceng.lazyclaude.session.published;

public final class OpeningPromptFixture {

    private OpeningPromptFixture() {}

    public static OpeningPrompt noFirstPrompt() {
        return new NoFirstPrompt();
    }

    public static OpeningPrompt openingUnder(SessionTitle title) {
        return title instanceof FirstPrompt prompt ? prompt : noFirstPrompt();
    }
}
