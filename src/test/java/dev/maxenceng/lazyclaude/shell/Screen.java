package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.RenderThreadForTests;

final class Screen {

    private final List<String> lines;

    private Screen(List<String> lines) {
        this.lines = lines;
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
        return new Screen(lines);
    }

    String text() {
        return String.join("\n", lines);
    }
}
