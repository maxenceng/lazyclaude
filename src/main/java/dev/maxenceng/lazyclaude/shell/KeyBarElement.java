package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** The bottom row: the keys that work now, dim, as many whole ones as the width holds, so a narrow terminal drops the last hints rather than cutting one in half. */
final class KeyBarElement implements Element {

    private static final String INDENT = " ";
    private static final String SEPARATOR = "  ";
    private static final int HEIGHT = 1;

    private final List<String> keys;

    KeyBarElement(List<String> keys) {
        this.keys = keys;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.height() <= 0 || area.width() <= 0 || keys.isEmpty()) {
            return;
        }
        String line = RowsElement.fit(INDENT + fitting(area.width()), area.width());
        frame.buffer().setString(area.x(), area.y(), line, Style.EMPTY.dim());
    }

    /** The longest run of keys from the first that fits; the first alone when even that is too wide, so the bar is never blank. */
    private String fitting(int width) {
        return IntStream.rangeClosed(1, keys.size())
                .mapToObj(count -> String.join(SEPARATOR, keys.subList(0, count)))
                .takeWhile(line -> INDENT.length() + line.codePointCount(0, line.length()) <= width)
                .reduce((_, longer) -> longer)
                .orElse(keys.getFirst());
    }

    @Override
    public Constraint constraint() {
        return Constraint.length(HEIGHT);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }
}
