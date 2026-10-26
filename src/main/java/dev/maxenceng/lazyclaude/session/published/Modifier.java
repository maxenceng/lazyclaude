package dev.maxenceng.lazyclaude.session.published;

/** A modifier key. */
public enum Modifier {
    CTRL("ctrl"),
    ALT("alt"),
    SHIFT("shift");

    private final String spelling;

    Modifier(String spelling) {
        this.spelling = spelling;
    }

    /** How a chord spells it, as Claude Code's {@code keybindings.json} does. */
    public String spelling() {
        return spelling;
    }
}
