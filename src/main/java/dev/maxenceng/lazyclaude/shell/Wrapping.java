package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

/** Breaks text into lines of at most a width, at the last space that fits, else in the middle of a word too long for a line. */
final class Wrapping {

    private static final char SPACE = ' ';
    private static final int AT_LEAST = 1;

    private Wrapping() {}

    /** The lines of {@code text}, a blank line staying blank, each of at most {@code width} characters. */
    static List<String> lines(String text, int width) {
        int room = Math.max(AT_LEAST, width);
        return text.lines().flatMap(line -> wrapped(line, room).stream()).toList();
    }

    private static List<String> wrapped(String line, int width) {
        if (line.length() <= width) {
            return List.of(line);
        }
        int space = line.lastIndexOf(SPACE, width);
        int cut = space > 0 ? space : width;
        return Stream.concat(Stream.of(line.substring(0, cut).stripTrailing()), wrapped(line.substring(cut).stripLeading(), width).stream())
                .toList();
    }
}
