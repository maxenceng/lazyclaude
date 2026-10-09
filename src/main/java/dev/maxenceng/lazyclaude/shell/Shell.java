package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;

/**
 * The terminal application itself. It owns the screen and the keyboard, and composes what
 * each context's application layer offers; it holds no rules of its own.
 */
public final class Shell extends ToolkitApp {

    static final String TITLE = "lazyclaude";

    @Override
    protected Element render() {
        return panel(
                        TITLE,
                        column(
                                text("Nothing here yet: docs/backlog decides what comes first."),
                                spacer(),
                                text("q quit").dim()))
                .rounded();
    }
}
