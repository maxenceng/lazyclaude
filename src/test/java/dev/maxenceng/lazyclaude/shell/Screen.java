package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.RenderThreadForTests;

public final class Screen {

    private static final int WIDTH = 80;
    private static final int HEIGHT = 24;
    private static final int LEFT_COLUMN_WIDTH = 28;
    private static final char BORDER = '│';
    private static final String BOTTOM_LEFT_CORNER = "╰";
    private static final String DOCK_BORDER = "╭dock";

    private final List<String> lines;
    private final Buffer buffer;
    private final Optional<Position> cursor;

    private Screen(List<String> lines, Buffer buffer, Optional<Position> cursor) {
        this.lines = lines;
        this.buffer = buffer;
        this.cursor = cursor;
    }

    public static Screen of(Shell shell) {
        return of(shell.render(), WIDTH, HEIGHT);
    }

    static Screen of(Element element, int width, int height) {
        return of(element, width, height, Rect.of(width, height));
    }

    static Screen of(Element element, int width, int height, Rect area) {
        Buffer buffer = Buffer.empty(Rect.of(width, height));
        Frame frame = Frame.forTesting(buffer);
        RenderThreadForTests.runAsRenderThread(() -> element.render(frame, area, DefaultRenderContext.createEmpty()));
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < width; x++) {
                line.append(buffer.get(x, y).symbol());
            }
            lines.add(line.toString());
        }
        return new Screen(lines, buffer, frame.cursorPosition());
    }

    List<String> boardPanelLines() {
        List<String> found = new ArrayList<>();
        for (String wholeLine : lines) {
            String line = wholeLine.substring(0, LEFT_COLUMN_WIDTH);
            int open = line.indexOf(BORDER);
            int close = open < 0 ? -1 : line.indexOf(BORDER, open + 1);
            if (close >= 0) {
                found.add(line.substring(open + 1, close).stripTrailing());
            }
        }
        return found;
    }

    public String boardPanelText() {
        return String.join("\n", boardPanelLines());
    }

    List<String> rightColumnLines() {
        return lines.stream().map(line -> line.substring(LEFT_COLUMN_WIDTH)).toList();
    }

    List<String> mainViewRows() {
        List<String> right = rightColumnLines();
        List<String> rows = new ArrayList<>();
        for (int y = 2; y < right.size() && !right.get(y).startsWith(BOTTOM_LEFT_CORNER); y++) {
            String line = right.get(y);
            rows.add(line.substring(1, line.length() - 1).stripTrailing());
        }
        return rows;
    }

    String text() {
        return String.join("\n", lines);
    }

    public String statusLine() {
        return lines.getFirst();
    }

    String dockTitle() {
        return lines.get(lineOf(DOCK_BORDER));
    }

    Position dockContentOrigin() {
        int line = lineOf(DOCK_BORDER);
        return new Position(lines.get(line).indexOf(DOCK_BORDER) + 1, line + 1);
    }

    String reversedTextOnLine(int line) {
        StringBuilder found = new StringBuilder();
        for (int x = 0; x < Math.min(WIDTH, lines.get(line).length()); x++) {
            Cell cell = buffer.get(x, line);
            if (cell.style().effectiveModifiers().contains(Modifier.REVERSED)) {
                found.append(cell.symbol());
            }
        }
        return found.toString().strip();
    }

    Optional<Position> cursor() {
        return cursor;
    }

    List<String> lines() {
        return lines;
    }

    List<String> reversedText() {
        return textOfCells(LEFT_COLUMN_WIDTH, cell -> cell.style().effectiveModifiers().contains(Modifier.REVERSED));
    }

    public List<String> dimText() {
        return textOfCells(WIDTH, cell -> cell.style().effectiveModifiers().contains(Modifier.DIM));
    }

    List<String> cyanText() {
        return textOfCells(WIDTH, cell -> cell.style().fg().filter(Color.CYAN::equals).isPresent());
    }

    List<String> foregroundText(Color color) {
        return textOfCells(WIDTH, cell -> cell.style().fg().filter(color::equals).isPresent());
    }

    int lineOf(String fragment) {
        return IntStream.range(0, lines.size())
                .filter(y -> lines.get(y).contains(fragment))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no line holds " + fragment));
    }

    String foregroundTextOnLine(int line, Color color) {
        StringBuilder found = new StringBuilder();
        for (int x = 0; x < Math.min(WIDTH, lines.get(line).length()); x++) {
            Cell cell = buffer.get(x, line);
            if (cell.style().fg().filter(color::equals).isPresent()) {
                found.append(cell.symbol());
            }
        }
        return found.toString().strip();
    }

    private List<String> textOfCells(int columns, Predicate<Cell> predicate) {
        List<String> found = new ArrayList<>();
        for (int y = 0; y < lines.size(); y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < Math.min(columns, lines.get(y).length()); x++) {
                Cell cell = buffer.get(x, y);
                if (predicate.test(cell)) {
                    line.append(cell.symbol());
                }
            }
            if (!line.isEmpty()) {
                found.add(line.toString().strip());
            }
        }
        return found;
    }
}
