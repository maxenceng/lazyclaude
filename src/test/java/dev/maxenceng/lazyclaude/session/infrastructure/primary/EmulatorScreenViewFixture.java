package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.published.CellColour;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.CursorAt;
import dev.maxenceng.lazyclaude.session.published.RunStyle;
import dev.maxenceng.lazyclaude.session.published.RunText;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.StyledRun;
import dev.maxenceng.lazyclaude.session.published.TextAttribute;

public final class EmulatorScreenViewFixture {

    private EmulatorScreenViewFixture() {}

    public static EmulatorScreenView blankScreen() {
        return EmulatorScreenView.builder().build();
    }

    public static EmulatorScreenView screenSaying(String... rows) {
        return EmulatorScreenView.builder()
                .rows(java.util.Arrays.stream(rows).map(EmulatorScreenViewFixture::plainRow).toList())
                .build();
    }

    public static EmulatorScreenView screenSayingWithCursorAt(int column, int row, String... rows) {
        return EmulatorScreenView.builder()
                .rows(java.util.Arrays.stream(rows).map(EmulatorScreenViewFixture::plainRow).toList())
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
