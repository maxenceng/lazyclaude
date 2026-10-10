package dev.maxenceng.lazyclaude.board.published;

public final class TitleFixture {

    private TitleFixture() {}

    public static Title title() {
        return new Title("A board from the backlog");
    }

    public static Title secondTitle() {
        return new Title("Plan the next slice");
    }
}
