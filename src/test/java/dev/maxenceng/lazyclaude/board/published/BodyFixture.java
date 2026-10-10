package dev.maxenceng.lazyclaude.board.published;

public final class BodyFixture {

    private BodyFixture() {}

    public static Body body() {
        return new Body("\n# A board from the backlog\n\nBody.");
    }

    public static Body secondBody() {
        return new Body("\n# Plan the next slice");
    }
}
