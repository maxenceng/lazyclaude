package dev.maxenceng.lazyclaude.session.published;

public final class SessionTitleFixture {

    private SessionTitleFixture() {}

    public static AiTitle aiTitle() {
        return new AiTitle("Refine 1");
    }

    public static FirstPrompt firstPrompt() {
        return new FirstPrompt("/ticket start 1");
    }

    public static SessionTitle noTitle() {
        return new NoTitle();
    }
}
