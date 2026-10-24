package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.AddRemoteControlView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConflictingRemoteControlView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RemoteControlConfigView;

/** How the shell words the popup that offers kitty's remote control lines, or shows what conflicts with them. */
final class RemoteControlPromptText {

    private static final Path HOME = Path.of(System.getProperty("user.home"));
    private static final String HOME_MARK = "~/";
    private static final String OFFER_TITLE = "kitty remote control is off";
    private static final String CONFLICT_TITLE = "kitty remote control conflicts";
    private static final String APPEND_TO = "append to %s:";
    private static final String INDENTED = "  %s";
    private static final String RESTART = "kitty needs a restart afterwards";
    private static final String SETS = "%s line %d sets";
    private static final String NEEDS = "remote control needs";
    private static final String NOT_EDITED = "nothing was edited";
    private static final String OFFER_HINT = "y add · n cancel";
    private static final String CONFLICT_HINT = "esc close";
    private static final String OFFER_KEYS = "y add  n/esc cancel";
    private static final String CONFLICT_KEYS = "esc close";
    private static final String BLANK = "";

    private RemoteControlPromptText() {}

    static String title(RemoteControlConfigView view) {
        return switch (view) {
            case AddRemoteControlView _ -> OFFER_TITLE;
            case ConflictingRemoteControlView _ -> CONFLICT_TITLE;
        };
    }

    static List<String> rows(RemoteControlConfigView view) {
        return switch (view) {
            case AddRemoteControlView adding ->
                Stream.of(
                                Stream.of(APPEND_TO.formatted(file(adding.file())), BLANK),
                                adding.lines().stream().map(INDENTED::formatted),
                                Stream.of(BLANK, RESTART))
                        .flatMap(rows -> rows)
                        .toList();
            case ConflictingRemoteControlView conflicting ->
                List.of(
                        SETS.formatted(file(conflicting.file()), conflicting.line()),
                        INDENTED.formatted(conflicting.setting()),
                        NEEDS,
                        INDENTED.formatted(conflicting.wanted()),
                        NOT_EDITED);
        };
    }

    static String hint(RemoteControlConfigView view) {
        return switch (view) {
            case AddRemoteControlView _ -> OFFER_HINT;
            case ConflictingRemoteControlView _ -> CONFLICT_HINT;
        };
    }

    static String keys(RemoteControlConfigView view) {
        return switch (view) {
            case AddRemoteControlView _ -> OFFER_KEYS;
            case ConflictingRemoteControlView _ -> CONFLICT_KEYS;
        };
    }

    private static String file(Path file) {
        return file.startsWith(HOME) ? HOME_MARK + HOME.relativize(file) : file.toString();
    }
}
