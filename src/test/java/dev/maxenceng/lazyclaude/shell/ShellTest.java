package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShellTest {

    @Test
    void shouldTitleTheScreenWithTheAppName() {
        Screen screen = Screen.of(new Shell().render(), 60, 8);

        assertThat(screen.text()).contains(Shell.TITLE);
    }

    @Test
    void shouldTellTheUserHowToQuit() {
        Screen screen = Screen.of(new Shell().render(), 60, 8);

        assertThat(screen.text()).as("a TUI with no visible way out traps whoever opened it").contains("q quit");
    }
}
