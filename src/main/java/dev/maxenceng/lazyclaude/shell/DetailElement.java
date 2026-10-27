package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.MarkdownView;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/**
 * Draws the main view's detail as one scrolling column: some plain head rows, then markdown. It
 * reports its height to the {@link MainViewScroll} on every render, so the scroll limit is the
 * height it actually drew.
 */
final class DetailElement implements Element {

    private final IntFunction<List<DetailRow>> head;
    private final Optional<String> markdown;
    private final MainViewScroll scroll;

    /** {@code head} says which rows precede the markdown, given the width they are drawn at. */
    DetailElement(IntFunction<List<DetailRow>> head, Optional<String> markdown, MainViewScroll scroll) {
        this.head = head;
        this.markdown = markdown;
        this.scroll = scroll;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (Areas.hasNoRoom(area)) {
            return;
        }
        List<DetailRow> head = this.head.apply(area.width());
        int bodyHeight = markdown.map(source -> view(source, 0).computeHeight(area.width())).orElse(0);
        scroll.observe(head.size() + bodyHeight, area.height());
        int offset = scroll.offset();
        IntStream.range(Math.max(0, offset), Math.min(head.size(), offset + area.height()))
                .forEach(row -> frame.buffer().setString(
                        area.x(), area.y() + row - offset, RowsElement.fit(head.get(row).text(), area.width()), head.get(row).style()));
        int bodyTop = head.size() - offset;
        if (bodyTop < area.height()) {
            int shown = Math.max(0, bodyTop);
            Rect bodyArea = new Rect(area.x(), area.y() + shown, area.width(), area.height() - shown);
            markdown.ifPresent(source -> view(source, Math.max(0, -bodyTop)).render(bodyArea, frame.buffer()));
        }
    }

    private static MarkdownView view(String source, int scrolled) {
        return MarkdownView.builder().source(source).scroll(scrolled).build();
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }
}
