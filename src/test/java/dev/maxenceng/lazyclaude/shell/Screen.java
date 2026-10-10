package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
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

    private final List<String> lines;
    private final Buffer buffer;

    private Screen(List<String> lines, Buffer buffer) {
        this.lines = lines;
        this.buffer = buffer;
    }

    public static Screen of(Shell shell) {
        return of(shell.render(), WIDTH, HEIGHT);
    }

    static Screen of(Element element, int width, int height) {
        Buffer buffer = Buffer.empty(Rect.of(width, height));
        Frame frame = Frame.forTesting(buffer);
        RenderThreadForTests.runAsRenderThread(
                () -> element.render(frame, frame.area(), DefaultRenderContext.createEmpty()));
        List<String> lines = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < width; x++) {
                line.append(buffer.get(x, y).symbol());
            }
            lines.add(line.toString());
        }
        return new Screen(lines, buffer);
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
