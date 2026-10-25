package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;

final class RowLineFixture {

    private RowLineFixture() {}

    static List<RowLine> rowLinesNumbered(int count) {
        return IntStream.range(0, count).mapToObj(index -> RowLine.plain("row " + index)).toList();
    }
}
