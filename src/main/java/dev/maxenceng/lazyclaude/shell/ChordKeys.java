package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.published.Chord;
import dev.maxenceng.lazyclaude.session.published.ReservedAction;
import dev.maxenceng.lazyclaude.session.published.ReservedChords;

/**
 * The reserved chords as the key bar spells them. Chords that share their modifiers and are each one character show the
 * modifiers once, {@code alt+h/j/k/l}; any others are each in full, joined by {@code /}.
 */
final class ChordKeys {

    private static final String JOINER = "/";

    private final ReservedChords chords;

    ChordKeys(ReservedChords chords) {
        this.chords = chords;
    }

    /** The four chords that move focus between panels, the main view and the dock. */
    String focus() {
        return spelled(ReservedAction.FOCUS_LEFT, ReservedAction.FOCUS_DOWN, ReservedAction.FOCUS_UP, ReservedAction.FOCUS_RIGHT);
    }

    /** The two chords that also step through the dock's tabs. */
    String tab() {
        return spelled(ReservedAction.FOCUS_LEFT, ReservedAction.FOCUS_RIGHT);
    }

    String hideMainView() {
        return chords.of(ReservedAction.HIDE_MAIN_VIEW).text();
    }

    String zoom() {
        return chords.of(ReservedAction.ZOOM).text();
    }

    private String spelled(ReservedAction... actions) {
        List<Chord> chosen = Stream.of(actions).map(chords::of).toList();
        return shareTheirModifiers(chosen)
                ? Stream.concat(Stream.of(chosen.getFirst().text()), chosen.stream().skip(1).map(Chord::keyText))
                        .collect(Collectors.joining(JOINER))
                : chosen.stream().map(Chord::text).collect(Collectors.joining(JOINER));
    }

    private static boolean shareTheirModifiers(List<Chord> chosen) {
        return chosen.stream().allMatch(Chord::isCharacter)
                && chosen.stream().map(Chord::modifiers).distinct().count() == 1;
    }
}
