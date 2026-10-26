package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.InvalidChordException;
import dev.maxenceng.lazyclaude.session.published.KeyInput;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;

/**
 * Reads a chord from its spelling, {@code ctrl+alt+h}. The configuration takes {@code ctrl}, {@code alt} and {@code shift} only;
 * Claude Code's {@code keybindings.json} also says {@code control}, and {@code meta}, {@code opt} or {@code option} for {@code alt},
 * names {@code esc}, {@code return} and {@code space} keys, in any case, and may bind a sequence of keystrokes, of which the first is the one that could shadow a reserved chord.
 */
final class ChordSpelling {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SEPARATOR = Pattern.compile("+", Pattern.LITERAL);
    private static final int KEEP_EMPTY = -1;
    private static final Map<String, Modifier> CONFIGURED = Map.of(
            Modifier.CTRL.spelling(), Modifier.CTRL, Modifier.ALT.spelling(), Modifier.ALT, Modifier.SHIFT.spelling(), Modifier.SHIFT);
    private static final Map<String, Modifier> CLAUDES = Map.of(
            Modifier.CTRL.spelling(), Modifier.CTRL,
            "control", Modifier.CTRL,
            Modifier.ALT.spelling(), Modifier.ALT,
            "opt", Modifier.ALT,
            "option", Modifier.ALT,
            "meta", Modifier.ALT,
            Modifier.SHIFT.spelling(), Modifier.SHIFT);
    private static final Map<String, KeyInput> CONFIGURED_KEYS = Map.of();
    private static final Map<String, KeyInput> CLAUDES_KEYS = Map.of(
            "esc", new PressedKey(NamedKey.ESCAPE),
            "return", new PressedKey(NamedKey.ENTER),
            "space", new TypedCharacter(' '));
    private static final String SEVERAL_KEYSTROKES = "'%s' is several keystrokes: a chord is one";
    private static final String NOT_A_MODIFIER = "'%s' is not a modifier: use ctrl, alt or shift";
    private static final String NOT_A_KEY = "'%s' is not a key";
    private static final String NO_KEY = "'%s' has no key";

    private ChordSpelling() {}

    /** @throws InvalidChordException when {@code text} is not one chord of ctrl, alt and shift and a key the terminal delivers */
    static Chord configured(String text) {
        String stripped = text.strip();
        if (WHITESPACE.splitAsStream(stripped).count() > 1) {
            throw new InvalidChordException(SEVERAL_KEYSTROKES.formatted(stripped));
        }
        return ChordDelivery.delivered(chordOf(stripped, CONFIGURED, CONFIGURED_KEYS));
    }

    /** The chord a Claude Code binding starts with, none when no chord of this configuration could be it. */
    static Optional<Chord> boundByClaude(String binding) {
        try {
            return WHITESPACE.splitAsStream(binding.strip()).findFirst().map(first -> chordOf(first, CLAUDES, CLAUDES_KEYS));
        } catch (InvalidChordException notOurs) {
            return Optional.empty();
        }
    }

    // deferred: a chord on the `+` key itself (`alt++`) is refused as having no key — close it when someone wants to bind `+`.
    private static Chord chordOf(String keystroke, Map<String, Modifier> modifiers, Map<String, KeyInput> keys) {
        List<String> parts = Arrays.asList(SEPARATOR.split(keystroke, KEEP_EMPTY));
        String key = parts.getLast();
        return Chord.builder()
                .modifiers(new Modifiers(parts.subList(0, parts.size() - 1).stream()
                        .map(word -> modifier(word, modifiers))
                        .toList()))
                .key(keyOf(key, keystroke, keys))
                .build();
    }

    private static Modifier modifier(String word, Map<String, Modifier> modifiers) {
        return Optional.ofNullable(modifiers.get(word.toLowerCase(Locale.ROOT)))
                .orElseThrow(() -> new InvalidChordException(NOT_A_MODIFIER.formatted(word)));
    }

    private static KeyInput keyOf(String key, String keystroke, Map<String, KeyInput> keys) {
        if (key.isEmpty()) {
            throw new InvalidChordException(NO_KEY.formatted(keystroke));
        }
        if (keys.containsKey(key.toLowerCase(Locale.ROOT))) {
            return keys.get(key.toLowerCase(Locale.ROOT));
        }
        if (key.codePointCount(0, key.length()) == 1) {
            return new TypedCharacter(key.codePointAt(0));
        }
        return Arrays.stream(NamedKey.values())
                .filter(named -> named.spelling().equals(key.toLowerCase(Locale.ROOT)))
                .findFirst()
                .<KeyInput>map(PressedKey::new)
                .orElseThrow(() -> new InvalidChordException(NOT_A_KEY.formatted(key)));
    }
}
