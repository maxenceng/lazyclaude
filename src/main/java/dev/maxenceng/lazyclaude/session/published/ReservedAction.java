package dev.maxenceng.lazyclaude.session.published;

/** What a reserved chord does, named as the configuration names it. */
public enum ReservedAction {
    FOCUS_LEFT("focusLeft", 'h'),
    FOCUS_DOWN("focusDown", 'j'),
    FOCUS_UP("focusUp", 'k'),
    FOCUS_RIGHT("focusRight", 'l'),
    HIDE_MAIN_VIEW("hideMainView", 'm'),
    ZOOM("zoom", 'z'),
    NEXT_WAITING("nextWaiting", 'w');

    private final String configurationName;
    private final char defaultKey;

    ReservedAction(String configurationName, char defaultKey) {
        this.configurationName = configurationName;
        this.defaultKey = defaultKey;
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
