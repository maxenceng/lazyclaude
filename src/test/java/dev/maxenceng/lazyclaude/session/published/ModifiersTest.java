package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModifiersTest {

    @Test
    void shouldSayWhichModifiersAreHeld() {
        Modifiers held = Modifiers.of(Modifier.CTRL, Modifier.SHIFT);

        assertThat(held.has(Modifier.CTRL)).isTrue();
        assertThat(held.has(Modifier.SHIFT)).isTrue();
        assertThat(held.has(Modifier.ALT)).isFalse();
    }

    @Test
    void shouldBeEmptyWhenNothingIsHeld() {
        assertThat(Modifiers.none().isEmpty()).isTrue();
        assertThat(Modifiers.of(Modifier.ALT).isEmpty()).isFalse();
    }
}
