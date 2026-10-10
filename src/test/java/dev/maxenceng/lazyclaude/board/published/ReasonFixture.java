package dev.maxenceng.lazyclaude.board.published;

public final class ReasonFixture {

    private ReasonFixture() {}

    public static Reason reason() {
        return new Reason("status is missing");
    }
}
