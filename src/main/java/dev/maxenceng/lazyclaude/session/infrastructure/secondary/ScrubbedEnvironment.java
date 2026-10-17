package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The environment a hosted {@code claude} runs in: lazyclaude's own, less every variable that would make it believe it runs
 * inside another Claude Code session, kitty or tmux, on an xterm with truecolor, which is what the emulator is. Claude Code's
 * sign-in and configuration variables pass through, so a hosted {@code claude} is signed in as the outer one is.
 */
final class ScrubbedEnvironment {

    private static final List<String> INHERITED_FROM_THE_OUTER_TERMINAL = List.of("KITTY", "TMUX", "TERMINFO", "TERM_PROGRAM");
    private static final Set<String> MARKING_A_NESTED_SESSION = Set.of(
            "CLAUDECODE",
            "CLAUDE_CODE_CHILD_SESSION",
            "CLAUDE_CODE_ENTRYPOINT",
            "CLAUDE_CODE_EXECPATH",
            "CLAUDE_CODE_MESSAGING_SOCKET",
            "CLAUDE_CODE_MESSAGING_TOKEN",
            "CLAUDE_CODE_SESSION_ATTENDED",
            "CLAUDE_CODE_SESSION_ID",
            "CLAUDE_CODE_SSE_PORT",
            "CLAUDE_EFFORT",
            "CLAUDE_PID");
    private static final String TERM = "TERM";
    private static final String COLORTERM = "COLORTERM";
    private static final String EMULATED_TERM = "xterm-256color";
    private static final String TRUECOLOR = "truecolor";

    private ScrubbedEnvironment() {}

    static Map<String, String> of(Map<String, String> environment) {
        Map<String, String> emulated = Map.of(TERM, EMULATED_TERM, COLORTERM, TRUECOLOR);
        return Stream.concat(
                        environment.entrySet().stream()
                                .filter(variable -> !isOfTheOuterTerminal(variable.getKey())
                                        && !MARKING_A_NESTED_SESSION.contains(variable.getKey())
                                        && !emulated.containsKey(variable.getKey())),
                        emulated.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isOfTheOuterTerminal(String name) {
        return INHERITED_FROM_THE_OUTER_TERMINAL.stream().anyMatch(name::startsWith);
    }
}
