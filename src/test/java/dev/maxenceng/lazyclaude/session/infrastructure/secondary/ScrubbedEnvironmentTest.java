package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ScrubbedEnvironmentTest {

    @Test
    void shouldDropEveryClaudeVariable() {
        Map<String, String> scrubbed = ScrubbedEnvironment.of(Map.of(
                "CLAUDECODE", "1", "CLAUDE_CODE_ENTRYPOINT", "cli", "CLAUDE_CODE_SSE_PORT", "1234", "HOME", "/home/max"));

        assertThat(scrubbed).containsOnlyKeys("HOME", "TERM", "COLORTERM");
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
