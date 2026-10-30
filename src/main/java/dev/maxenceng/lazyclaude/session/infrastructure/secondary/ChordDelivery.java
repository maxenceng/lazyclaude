package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.InvalidChordException;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.PressedKey;
import dev.maxenceng.lazyclaude.session.published.TypedCharacter;

/**
 * Refuses a configured chord the outer terminal never delivers as itself, so it could never be pressed while its old chord went to
 * {@code claude}. Without the kitty keyboard protocol a terminal sends {@code ctrl} with a letter as one control byte, shared by
 * {@code ctrl+h} and backspace, {@code ctrl+i} and tab, {@code ctrl+j}, {@code ctrl+m} and enter, {@code ctrl+[} and escape; it
 * sends {@code shift} as the character it types; and TamboUI reads neither {@code ctrl} with {@code alt} on a character, nor a
 * modifier on enter, escape, backspace or tab, nor {@code alt+[}, which starts an escape sequence.
 */
final class ChordDelivery {

    private static final String NEVER_DELIVERED = "'%s' never reaches lazyclaude: %s";
    private static final String SENT_AS = "the terminal sends it as %s";
    private static final String SHIFTED = "the terminal sends shift as the character it types";
    private static final String CTRL_AND_ALT = "the terminal cannot report ctrl and alt together on a character";
    private static final String CTRL_ON = "the terminal cannot report ctrl on '%s'";
    private static final String MODIFIED = "the terminal cannot report a modifier on %s";
    private static final String ESCAPE_SEQUENCE = "the terminal sends it as the start of an escape sequence";
    private static final String CTRL_CHARACTERS = "abcdefgklnopqrstuvwxyz \\]^/";
    private static final int ESCAPE_SEQUENCE_START = '[';
    private static final Map<Integer, NamedKey> CTRL_SENT_AS = Map.of(
            (int) 'h', NamedKey.BACKSPACE,
            (int) 'i', NamedKey.TAB,
            (int) 'j', NamedKey.ENTER,
            (int) 'm', NamedKey.ENTER,
            ESCAPE_SEQUENCE_START, NamedKey.ESCAPE);
    private static final List<NamedKey> UNMODIFIABLE = List.of(NamedKey.ENTER, NamedKey.ESCAPE, NamedKey.BACKSPACE, NamedKey.TAB);

    private ChordDelivery() {}

    // deferred: a chord the terminal sends as another key, or that TamboUI 0.5.0 cannot parse, is refused rather than bound — close it when TamboUI supports the kitty keyboard protocol, an open TamboUI issue.
    /** @throws InvalidChordException when the terminal never delivers {@code chord} as itself */
    static Chord delivered(Chord chord) {
        undeliveredBecause(chord).ifPresent(reason -> {
            throw new InvalidChordException(NEVER_DELIVERED.formatted(chord.text(), reason));
        });
        return chord;
    }

    private static Optional<String> undeliveredBecause(Chord chord) {
        return switch (chord.key()) {
            case PressedKey pressed -> UNMODIFIABLE.contains(pressed.key())
                    ? Optional.of(MODIFIED.formatted(pressed.key().spelling()))
                    : Optional.empty();
            case TypedCharacter typed -> characterUndeliveredBecause(chord, typed.codePoint());
        };
    }

    private static Optional<String> characterUndeliveredBecause(Chord chord, int codePoint) {
        boolean ctrl = chord.modifiers().has(Modifier.CTRL);
        if (chord.modifiers().has(Modifier.SHIFT)) {
            return Optional.of(SHIFTED);
        }
        if (ctrl && chord.modifiers().has(Modifier.ALT)) {
            return Optional.of(CTRL_AND_ALT);
        }
        if (ctrl && CTRL_SENT_AS.containsKey(codePoint)) {
            return Optional.of(SENT_AS.formatted(CTRL_SENT_AS.get(codePoint).spelling()));
        }
        if (ctrl && !CTRL_CHARACTERS.contains(Character.toString(codePoint))) {
            return Optional.of(CTRL_ON.formatted(chord.keyText()));
        }
        return !ctrl && codePoint == ESCAPE_SEQUENCE_START ? Optional.of(ESCAPE_SEQUENCE) : Optional.empty();
    }
}
