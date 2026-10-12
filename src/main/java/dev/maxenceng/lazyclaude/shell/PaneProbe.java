package dev.maxenceng.lazyclaude.shell;

import java.util.function.Consumer;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

/** Draws its child in the area it is given and says which area that was, since the pty takes the size of the pane. */
final class PaneProbe implements Element {

    private final Element child;
    private final Consumer<Rect> onArea;

    PaneProbe(Element child, Consumer<Rect> onArea) {
        this.child = child;
        this.onArea = onArea;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        onArea.accept(area);
        context.renderChild(child, frame, area);
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
