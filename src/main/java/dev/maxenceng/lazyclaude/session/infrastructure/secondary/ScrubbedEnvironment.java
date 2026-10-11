package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The environment a hosted {@code claude} runs in: lazyclaude's own, less every variable that would make it believe it runs
 * inside another Claude Code session, kitty or tmux, on an xterm with truecolor, which is what the emulator is.
 */
final class ScrubbedEnvironment {

    private static final List<String> INHERITED_FROM_THE_OUTER_TERMINAL =
            List.of("CLAUDE", "KITTY", "TMUX", "TERMINFO");
    private static final String TERM = "TERM";
    private static final String COLORTERM = "COLORTERM";
    private static final String EMULATED_TERM = "xterm-256color";
    private static final String TRUECOLOR = "truecolor";

    private ScrubbedEnvironment() {}

    static Map<String, String> of(Map<String, String> environment) {
        Map<String, String> scrubbed = new HashMap<>(environment);
        scrubbed.keySet().removeIf(ScrubbedEnvironment::isOfTheOuterTerminal);
        scrubbed.put(TERM, EMULATED_TERM);
        scrubbed.put(COLORTERM, TRUECOLOR);
        return scrubbed;
    }

    private static boolean isOfTheOuterTerminal(String name) {
        return INHERITED_FROM_THE_OUTER_TERMINAL.stream().anyMatch(name::startsWith);
    }
}
