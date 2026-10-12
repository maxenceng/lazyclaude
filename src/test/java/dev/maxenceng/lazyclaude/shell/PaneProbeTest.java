package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

class PaneProbeTest {

    @SuppressWarnings("unchecked")
    private final Consumer<Rect> onArea = mock(Consumer.class);

    @Test
    void shouldDrawItsChildAndSayWhereItDidSo() {
        Screen screen = Screen.of(new PaneProbe(text("inside"), onArea), 10, 2, new Rect(2, 1, 6, 1));

        assertThat(screen.lines()).containsExactly("          ", "  inside  ");
        verify(onArea).accept(new Rect(2, 1, 6, 1));
    }
}
