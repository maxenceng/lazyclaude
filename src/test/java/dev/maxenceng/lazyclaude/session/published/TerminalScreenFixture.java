package dev.maxenceng.lazyclaude.session.published;

import java.util.List;

public final class TerminalScreenFixture {

    private TerminalScreenFixture() {}

    public static TerminalScreen screenSaying(String... rows) {
        return TerminalScreen.builder()
                .rows(java.util.Arrays.stream(rows).map(TerminalScreenFixture::plainRow).toList())
                .build();
    }

    public static TerminalScreen screenSayingWithCursorAt(int column, int row, String... rows) {
        return TerminalScreen.builder()
                .rows(java.util.Arrays.stream(rows).map(TerminalScreenFixture::plainRow).toList())
                .cursor(CursorAt.builder()
                        .column(new CellColumn(column))
                        .row(new CellRow(row))
                        .build())
                .build();
    }

    public static ScreenRow plainRow(String text) {
        return new ScreenRow(List.of(run(text, RunStyle.builder().build())));
    }

    public static StyledRun run(String text, RunStyle style) {
        return StyledRun.builder().text(new RunText(text)).style(style).build();
    }

    public static RunStyle coloured(CellColour foreground) {
        return RunStyle.builder().foreground(foreground).build();
    }

    public static RunStyle styled(CellColour foreground, TextAttribute... attributes) {
        return RunStyle.builder()
                .foreground(foreground)
                .attributes(List.of(attributes))
                .build();
    }

    public static RunStyle with(TextAttribute... attributes) {
        return RunStyle.builder().attributes(List.of(attributes)).build();
    }
}
