package dev.maxenceng.lazyclaude.session.published;

import java.util.Locale;

/** A key that types no character of its own. */
public enum NamedKey {
    ENTER,
    ESCAPE,
    BACKSPACE,
    TAB,
    DELETE,
    INSERT,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    HOME,
    END,
    PAGE_UP,
    PAGE_DOWN,
    F1,
    F2,
    F3,
    F4,
    F5,
    F6,
    F7,
    F8,
    F9,
    F10,
    F11,
    F12;

    private static final String UNDERSCORE = "_";
    private static final String NOTHING = "";

    /** How a chord spells the key: {@code pageup}, {@code f5}, as Claude Code's {@code keybindings.json} does. */
    public String spelling() {
        return name().replace(UNDERSCORE, NOTHING).toLowerCase(Locale.ROOT);
    }
}
