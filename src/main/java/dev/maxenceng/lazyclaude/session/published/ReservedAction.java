package dev.maxenceng.lazyclaude.session.published;

/** What a reserved chord does, named as the configuration names it. */
public enum ReservedAction {
    FOCUS_LEFT("focusLeft", 'h', false),
    FOCUS_DOWN("focusDown", 'j', false),
    FOCUS_UP("focusUp", 'k', false),
    FOCUS_RIGHT("focusRight", 'l', false),
    HIDE_MAIN_VIEW("hideMainView", 'm', false),
    ZOOM("zoom", 'z', false),
    NEXT_WAITING("nextWaiting", 'w', false),
    CLOSE("close", 'x', true);

    private final String configurationName;
    private final char defaultKey;
    private final boolean desktopOnly;

    ReservedAction(String configurationName, char defaultKey, boolean desktopOnly) {
        this.configurationName = configurationName;
        this.defaultKey = defaultKey;
        this.desktopOnly = desktopOnly;
    }

    /** Whether only the desktop frontend reserves this chord: the terminal one leaves it to {@code claude}, though a clash is still refused. */
    public boolean desktopOnly() {
        return desktopOnly;
    }

    public String configurationName() {
        return configurationName;
    }

    /** {@code alt} and the letter the layout decision gave it. */
    public Chord defaultChord() {
        return Chord.builder()
                .modifiers(Modifiers.of(Modifier.ALT))
                .key(new TypedCharacter(defaultKey))
                .build();
    }
}
