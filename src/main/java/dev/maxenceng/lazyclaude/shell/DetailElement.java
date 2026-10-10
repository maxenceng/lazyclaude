package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.MarkdownView;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/**
 * Draws a ticket's detail as one scrolling column: the facts line, the other frontmatter keys and a
 * rule, then the body as markdown. It reports its height to the {@link MainViewScroll} on every
 * render, so the scroll limit is the height it actually drew.
 */
final class DetailElement implements Element {

    private static final String RULE = "─";
    private static final String NO_BODY = "no body";

    private final String facts;
    private final List<String> keys;
    private final TicketBody body;
    private final MainViewScroll scroll;

    DetailElement(String facts, List<String> keys, TicketBody body, MainViewScroll scroll) {
        this.facts = facts;
        this.keys = keys;
        this.body = body;
        this.scroll = scroll;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.width() <= 0 || area.height() <= 0) {
            return;
        }
        List<DetailRow> head = head(area.width());
        Optional<String> markdown = markdown();
        int bodyHeight = markdown.map(source -> view(source, 0).computeHeight(area.width())).orElse(0);
        scroll.observe(head.size() + bodyHeight, area.height());
        int offset = scroll.offset();
        for (int row = 0; row < head.size(); row++) {
            int y = row - offset;
            if (y >= 0 && y < area.height()) {
                DetailRow detail = head.get(row);
                frame.buffer().setString(area.x(), area.y() + y, RowsElement.fit(detail.text(), area.width()), detail.style());
            }
        }
        int bodyTop = head.size() - offset;
        if (bodyTop < area.height()) {
            int shown = Math.max(0, bodyTop);
            Rect bodyArea = new Rect(area.x(), area.y() + shown, area.width(), area.height() - shown);
            markdown.ifPresent(source -> view(source, Math.max(0, -bodyTop)).render(bodyArea, frame.buffer()));
        }
    }

    private List<DetailRow> head(int width) {
        List<DetailRow> head = new ArrayList<>();
        head.add(new DetailRow(facts, Style.EMPTY.bold()));
        keys.forEach(key -> head.add(new DetailRow(key, Style.EMPTY)));
        head.add(new DetailRow(RULE.repeat(width), Style.EMPTY.dim()));
        if (body instanceof NoBody) {
            head.add(new DetailRow(NO_BODY, Style.EMPTY.dim()));
        }
        return head;
    }

    private Optional<String> markdown() {
        return switch (body) {
            case Body text -> Optional.of(text.value());
            case NoBody none -> Optional.empty();
        };
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
