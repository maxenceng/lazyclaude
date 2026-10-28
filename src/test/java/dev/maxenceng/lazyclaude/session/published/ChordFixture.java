package dev.maxenceng.lazyclaude.session.published;

import java.util.Map;

public final class ChordFixture {

    private ChordFixture() {}

    public static Chord altH() {
        return chord('h', Modifier.ALT);
    }

    public static Chord altJ() {
        return chord('j', Modifier.ALT);
    }

    public static Chord altK() {
        return chord('k', Modifier.ALT);
    }

    public static Chord altL() {
        return chord('l', Modifier.ALT);
    }

    public static Chord altM() {
        return chord('m', Modifier.ALT);
    }

    public static Chord altZ() {
        return chord('z', Modifier.ALT);
    }

    public static Chord altW() {
        return chord('w', Modifier.ALT);
    }

    public static Chord ctrlAltH() {
        return chord('h', Modifier.CTRL, Modifier.ALT);
    }

    public static Chord ctrlY() {
        return chord('y', Modifier.CTRL);
    }

    public static Chord ctrlC() {
        return chord('c', Modifier.CTRL);
    }

    public static Chord ctrlO() {
        return chord('o', Modifier.CTRL);
    }

    public static Chord chord(char key, Modifier... modifiers) {
        return Chord.builder().modifiers(Modifiers.of(modifiers)).key(new TypedCharacter(key)).build();
    }

    public static Chord namedChord(NamedKey key, Modifier... modifiers) {
        return Chord.builder().modifiers(Modifiers.of(modifiers)).key(new PressedKey(key)).build();
    }

    public static TypedCharacter typedKey(char key) {
        return new TypedCharacter(key);
    }

    public static Modifiers held(Modifier... modifiers) {
        return Modifiers.of(modifiers);
    }

    public static ReservedChords rebinding(Map<ReservedAction, Chord> changes) {
        return ReservedChords.defaults().rebinding(changes);
    }

    public static ReservedChords rebinding(ReservedAction action, Chord chord) {
        return rebinding(Map.of(action, chord));
    }
}
