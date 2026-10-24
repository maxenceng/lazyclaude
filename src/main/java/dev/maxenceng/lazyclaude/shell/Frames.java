package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.panel;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Panel;

/** The frame every panel of the screen is drawn in, its border cyan while it has focus. */
final class Frames {

    private Frames() {}

    static Panel framed(String title, Element body, boolean focused) {
        Panel panel = panel(title, body).rounded();
        return focused ? panel.borderColor(Color.CYAN) : panel;
    }
}
