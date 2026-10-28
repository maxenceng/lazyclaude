package dev.maxenceng.lazyclaude.session.published;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A key pressed with the modifiers that make it a chord. A bare key would steal what is typed into {@code claude}, and
 * {@code shift} alone only capitalises it, so neither is a chord. Modifiers are kept in the order {@link Modifier} lists
 * them and a character key by its lower case, so two spellings of one chord are equal.
 */
public record Chord(Modifiers modifiers, KeyInput key) {

    private static final String JOINER = "+";
    private static final String NO_MODIFIER = "'%s' has no modifier: add ctrl or alt";
    private static final String SHIFT_ALONE = "'%s' is shift alone: add ctrl or alt";

    public Chord {
        Assert.notNull("modifiers", modifiers);
        Assert.notNull("key", key);
        modifiers = canonical(modifiers);
        key = lowerCased(key);
        refuseWhenWeak(modifiers, key);
    }

    /**
     * Whether {@code pressed}, held with {@code held}, is this key with exactly these modifiers. The pressed key keeps its case: a terminal reports
     * {@code alt+shift+h} as {@code alt} and {@code H}, which is not this chord's {@code alt+h} and belongs to {@code claude}.
     */
    public boolean isPressedBy(KeyInput pressed, Modifiers held) {
        return modifiers.equals(canonical(held)) && key.equals(pressed);
    }

    /** The chord as the configuration spells it: {@code ctrl+alt+h}. */
    public String text() {
        return textOf(modifiers, key);
    }

    /** The key alone, as {@link #text} ends: {@code h}. */
    public String keyText() {
        return keyTextOf(key);
    }

    /** Whether the key is a single character, so that chords sharing their modifiers can show them once. */
    public boolean isCharacter() {
        return key instanceof TypedCharacter;
    }

    private static String textOf(Modifiers modifiers, KeyInput key) {
        return Stream.concat(modifiers.held().stream().map(Modifier::spelling), Stream.of(keyTextOf(key)))
                .collect(Collectors.joining(JOINER));
    }

    private static String keyTextOf(KeyInput key) {
        return switch (key) {
            case TypedCharacter typed -> new String(Character.toChars(typed.codePoint()));
            case PressedKey pressed -> pressed.key().spelling();
        };
    }

    private static Modifiers canonical(Modifiers held) {
        return new Modifiers(Stream.of(Modifier.values()).filter(held::has).toList());
    }

    private static KeyInput lowerCased(KeyInput input) {
        return switch (input) {
            case TypedCharacter typed -> new TypedCharacter(Character.toLowerCase(typed.codePoint()));
            case PressedKey pressed -> pressed;
        };
    }

    private static void refuseWhenWeak(Modifiers modifiers, KeyInput key) {
        if (modifiers.isEmpty()) {
            throw new InvalidChordException(NO_MODIFIER.formatted(textOf(modifiers, key)));
        }
        if (modifiers.equals(Modifiers.of(Modifier.SHIFT))) {
            throw new InvalidChordException(SHIFT_ALONE.formatted(textOf(modifiers, key)));
        }
    }

    public static ChordBuilder builder() {
        return new ChordBuilder();
    }

    public static final class ChordBuilder {
        private Modifiers modifiers;
        private KeyInput key;

        private ChordBuilder() {}

        public ChordBuilder modifiers(Modifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public ChordBuilder key(KeyInput key) {
            this.key = key;
            return this;
        }

        public Chord build() {
            return new Chord(modifiers, key);
        }
    }
}
