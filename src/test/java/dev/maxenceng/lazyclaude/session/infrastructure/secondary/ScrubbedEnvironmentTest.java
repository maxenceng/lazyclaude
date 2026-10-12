package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ScrubbedEnvironmentTest {

    @Test
    void shouldDropEveryVariableMarkingANestedClaudeSession() {
        Map<String, String> scrubbed = ScrubbedEnvironment.of(Map.ofEntries(
                Map.entry("CLAUDECODE", "1"),
                Map.entry("CLAUDE_CODE_CHILD_SESSION", "1"),
                Map.entry("CLAUDE_CODE_ENTRYPOINT", "cli"),
                Map.entry("CLAUDE_CODE_EXECPATH", "/usr/bin/claude"),
                Map.entry("CLAUDE_CODE_MESSAGING_SOCKET", "/tmp/claude.sock"),
                Map.entry("CLAUDE_CODE_MESSAGING_TOKEN", "t"),
                Map.entry("CLAUDE_CODE_SESSION_ATTENDED", "1"),
                Map.entry("CLAUDE_CODE_SESSION_ID", "a1b2"),
                Map.entry("CLAUDE_CODE_SSE_PORT", "1234"),
                Map.entry("CLAUDE_EFFORT", "high"),
                Map.entry("CLAUDE_PID", "42"),
                Map.entry("HOME", "/home/max")));

        assertThat(scrubbed).containsOnlyKeys("HOME", "TERM", "COLORTERM");
    }

    @Test
    void shouldKeepClaudesSignInAndConfigurationVariables() {
        assertThat(ScrubbedEnvironment.of(Map.of(
                        "CLAUDE_CODE_USE_BEDROCK", "1",
                        "CLAUDE_CODE_USE_VERTEX", "1",
                        "CLAUDE_CODE_OAUTH_TOKEN", "o",
                        "CLAUDE_CONFIG_DIR", "/home/max/.claude-work")))
                .as("a hosted claude must come up signed in and configured as the outer one is")
                .containsKeys("CLAUDE_CODE_USE_BEDROCK", "CLAUDE_CODE_USE_VERTEX", "CLAUDE_CODE_OAUTH_TOKEN", "CLAUDE_CONFIG_DIR");
    }

    @Test
    void shouldDropEveryKittyVariable() {
        Map<String, String> scrubbed = ScrubbedEnvironment.of(Map.of(
                "KITTY_WINDOW_ID", "3", "KITTY_PID", "99", "KITTY_PUBLIC_KEY", "x", "PATH", "/usr/bin"));

        assertThat(scrubbed).containsOnlyKeys("PATH", "TERM", "COLORTERM");
    }

    @Test
    void shouldDropTmux() {
        Map<String, String> scrubbed = ScrubbedEnvironment.of(Map.of("TMUX", "/tmp/tmux-1000/default,1,0", "TMUX_PANE", "%1", "LANG", "C"));

        assertThat(scrubbed).containsOnlyKeys("LANG", "TERM", "COLORTERM");
    }

    @Test
    void shouldDropTheTerminfoDirectoryOfTheOuterTerminal() {
        assertThat(ScrubbedEnvironment.of(Map.of("TERMINFO", "/usr/lib/kitty/terminfo"))).doesNotContainKey("TERMINFO");
    }

    @Test
    void shouldDropTheNameOfTheOuterTerminalProgram() {
        assertThat(ScrubbedEnvironment.of(Map.of("TERM_PROGRAM", "ghostty", "TERM_PROGRAM_VERSION", "1.2.0")))
                .as("claude picks terminal features by TERM_PROGRAM, and the emulator is not the outer terminal")
                .doesNotContainKeys("TERM_PROGRAM", "TERM_PROGRAM_VERSION");
    }

    @Test
    void shouldTellTheChildItRunsOnAnXtermWithTruecolor() {
        assertThat(ScrubbedEnvironment.of(Map.of("TERM", "xterm-kitty", "COLORTERM", "24bit")))
                .containsEntry("TERM", "xterm-256color")
                .containsEntry("COLORTERM", "truecolor");
    }

    @Test
    void shouldKeepWhatClaudeNeedsToFindItsConfiguration() {
        assertThat(ScrubbedEnvironment.of(Map.of("HOME", "/home/max", "PATH", "/usr/bin", "ANTHROPIC_API_KEY", "k")))
                .containsEntry("HOME", "/home/max")
                .containsEntry("PATH", "/usr/bin")
                .containsEntry("ANTHROPIC_API_KEY", "k");
    }
}
