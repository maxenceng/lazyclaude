package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Verb;

/** How the shell words the verb picker: its title, a row per verb with the cursor's mark, and the keys it answers to. */
final class VerbPickerText {

    private static final String TITLE_FORMAT = "%s · %s";
    private static final String CHECKING = " ── checking PR…";
    private static final String CURSOR = "› ";
    private static final String NO_CURSOR = "  ";
    private static final String HINT = "enter start · esc cancel";
    private static final String KEYS = "j/k move  enter start  esc cancel";

    private VerbPickerText() {}

    static String title(VerbPicker picker) {
        String head = TITLE_FORMAT.formatted(picker.ticket().id().value(), picker.ticket().status().label());
        return picker.checking() ? head + CHECKING : head;
    }

    static List<String> rows(VerbPicker picker) {
        List<Verb> verbs = picker.verbs();
        return verbs.stream()
                .map(verb -> (verb == picker.selected() ? CURSOR : NO_CURSOR) + verb.label())
                .toList();
    }

    static String hint() {
        return HINT;
    }

    static String keys() {
        return KEYS;
    }

    static int width(VerbPicker picker) {
        return Stream.concat(Stream.of(title(picker), hint()), rows(picker).stream())
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }
}
