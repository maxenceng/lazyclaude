package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSizeOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaneSizeTest {

    @Test
    void shouldKeepAWidthOfAtLeastOneCell() {
        assertThat(paneSizeOf(0, 24).width().cells())
                .as("a pty cannot be zero columns wide, and a pane squeezed to nothing must not ask for it")
                .isEqualTo(1);
    }

    @Test
    void shouldKeepAHeightOfAtLeastOneCell() {
        assertThat(paneSizeOf(80, -3).height().cells())
                .as("a pty cannot be zero rows tall, and a pane squeezed to nothing must not ask for it")
                .isEqualTo(1);
    }

    @Test
    void shouldKeepASizeThatFits() {
        assertThat(paneSizeOf(120, 40).width().cells()).isEqualTo(120);
    }
}
