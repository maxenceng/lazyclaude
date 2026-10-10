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

final class Screen {

    private final List<String> lines;
    private final Buffer buffer;

    private Screen(List<String> lines, Buffer buffer) {
        this.lines = lines;
        this.buffer = buffer;
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

    String text() {
        return String.join("\n", lines);
    }

    List<String> lines() {
        return lines;
    }

    List<String> reversedText() {
        return textOfCells(cell -> cell.style().effectiveModifiers().contains(Modifier.REVERSED));
    }

    List<String> cyanText() {
        return textOfCells(cell -> cell.style().fg().filter(Color.CYAN::equals).isPresent());
    }

    private List<String> textOfCells(Predicate<Cell> predicate) {
        List<String> found = new ArrayList<>();
        for (int y = 0; y < lines.size(); y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < lines.get(y).length(); x++) {
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
