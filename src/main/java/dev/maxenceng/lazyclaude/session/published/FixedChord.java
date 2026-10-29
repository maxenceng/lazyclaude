package dev.maxenceng.lazyclaude.session.published;

/**
 * A chord lazyclaude keeps for itself outside a hosted session and that {@code config.yml} cannot move. A reserved chord is
 * matched before it, so none may be rebound onto one: the fixed key would stop working.
 */
public enum FixedChord {
    QUIT('c', "quit"),
    HALF_PAGE_DOWN('d', "half page down"),
    HALF_PAGE_UP('u', "half page up");

    private final char key;
    private final String purpose;

    FixedChord(char key, String purpose) {
        this.key = key;
        this.purpose = purpose;
    }

    /** {@code ctrl} and its letter. */
    public Chord chord() {
        return Chord.builder()
                .modifiers(Modifiers.of(Modifier.CTRL))
                .key(new TypedCharacter(key))
                .build();
    }

    public boolean isPressedBy(KeyInput pressed, Modifiers held) {
        return chord().isPressedBy(pressed, held);
    }

    /** What it does, as a refusal names it: {@code quit}. */
    public String purpose() {
        return purpose;
    }
}
