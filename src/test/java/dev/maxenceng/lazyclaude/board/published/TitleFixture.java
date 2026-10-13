package dev.maxenceng.lazyclaude.board.published;

public final class TitleFixture {

    private TitleFixture() {}

    public static Title title() {
        return new Title("A board from the backlog");
    }

    public static Title secondTitle() {
        return new Title("Plan the next slice");
    }

    public static Title longTitle() {
        return new Title("PR and CI glyphs on the board and in the ticket detail");
    }
}
