package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class TerminalFocusTest {

    @Test
    void shouldCountAsFocusedUntilAFocusOutArrives() {
        assertThat(new TerminalFocus(mock(Runnable.class)).focused())
                .as("a terminal that never reports focus must not silence notifications")
                .isTrue();
    }

    @Test
    void shouldBeUnfocusedAfterAFocusOutAndFocusedAgainAfterAFocusIn() {
        TerminalFocus focus = new TerminalFocus(mock(Runnable.class));

        focus.lost();
        assertThat(focus.focused()).isFalse();
        focus.gained();

        assertThat(focus.focused()).isTrue();
    }

    @Test
    void shouldTellOnlyWhenFocusReallyMoved() {
        Runnable changed = mock(Runnable.class);
        TerminalFocus focus = new TerminalFocus(changed);

        focus.gained();
        verify(changed, never()).run();
        focus.lost();
        focus.lost();
        focus.gained();

        verify(changed, times(2)).run();
    }
}
