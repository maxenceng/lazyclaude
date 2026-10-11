package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.model.TerminalLine;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.util.CharUtils;

import dev.maxenceng.lazyclaude.session.published.CellColour;
import dev.maxenceng.lazyclaude.session.published.CellColumn;
import dev.maxenceng.lazyclaude.session.published.CellRow;
import dev.maxenceng.lazyclaude.session.published.CursorAt;
import dev.maxenceng.lazyclaude.session.published.DefaultColour;
import dev.maxenceng.lazyclaude.session.published.IndexedColour;
import dev.maxenceng.lazyclaude.session.published.NoCursor;
import dev.maxenceng.lazyclaude.session.published.RgbColour;
import dev.maxenceng.lazyclaude.session.published.RunStyle;
import dev.maxenceng.lazyclaude.session.published.RunText;
import dev.maxenceng.lazyclaude.session.published.ScreenCursor;
import dev.maxenceng.lazyclaude.session.published.ScreenRow;
import dev.maxenceng.lazyclaude.session.published.StyledRun;
import dev.maxenceng.lazyclaude.session.published.TerminalScreen;
import dev.maxenceng.lazyclaude.session.published.TextAttribute;

/** Copies the emulator's cell grid into a {@link TerminalScreen}, so that nothing of jediterm leaves this package. */
final class ScreenCopy {

    private static final int RGB_MASK = 0xFFFFFF;
    private static final String EMPTY_CELL = " ";
    private static final Map<TextStyle.Option, TextAttribute> ATTRIBUTES = attributes();

    private ScreenCopy() {}

    /** The screen as the buffer holds it right now, with the cursor at {@code column} and {@code row} when {@code cursorVisible}. */
    static TerminalScreen of(TerminalTextBuffer buffer, CellColumn column, CellRow row, boolean cursorVisible) {
        buffer.lock();
        try {
            return TerminalScreen.builder()
                    .rows(IntStream.range(0, buffer.getHeight())
                            .mapToObj(y -> new ScreenRow(runsOf(buffer.getLine(y))))
                            .toList())
                    .cursor(cursor(column, row, cursorVisible))
                    .build();
        } finally {
            buffer.unlock();
        }
    }

    private static ScreenCursor cursor(CellColumn column, CellRow row, boolean visible) {
        return visible ? CursorAt.builder().column(column).row(row).build() : new NoCursor();
    }

    private static List<StyledRun> runsOf(TerminalLine line) {
        return line.getEntries().stream()
                .map(entry -> run(text(entry.getText().toString()), entry.getStyle()))
                .flatMap(Optional::stream)
                .toList();
    }

    private static String text(String raw) {
        return raw.replace(CharUtils.NUL_CHAR + "", EMPTY_CELL).replace(CharUtils.DWC + "", "");
    }

    private static Optional<StyledRun> run(String text, TextStyle style) {
        if (text.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(StyledRun.builder().text(new RunText(text)).style(styleOf(style)).build());
    }

    private static RunStyle styleOf(TextStyle style) {
        return RunStyle.builder()
                .foreground(colour(style.getForeground()))
                .background(colour(style.getBackground()))
                .attributes(ATTRIBUTES.entrySet().stream()
                        .filter(attribute -> style.hasOption(attribute.getKey()))
                        .map(Map.Entry::getValue)
                        .toList())
                .build();
    }

    private static CellColour colour(TerminalColor colour) {
        if (colour == null) {
            return new DefaultColour();
        }
        return colour.isIndexed()
                ? new IndexedColour(colour.getColorIndex())
                : new RgbColour(colour.toColor().getRGB() & RGB_MASK);
    }

    private static Map<TextStyle.Option, TextAttribute> attributes() {
        Map<TextStyle.Option, TextAttribute> attributes = new EnumMap<>(TextStyle.Option.class);
        attributes.put(TextStyle.Option.BOLD, TextAttribute.BOLD);
        attributes.put(TextStyle.Option.DIM, TextAttribute.DIM);
        attributes.put(TextStyle.Option.ITALIC, TextAttribute.ITALIC);
        attributes.put(TextStyle.Option.UNDERLINED, TextAttribute.UNDERLINED);
        attributes.put(TextStyle.Option.INVERSE, TextAttribute.REVERSED);
        return attributes;
    }
}
