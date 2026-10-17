package dev.maxenceng.lazyclaude.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class KeyBarElementTest {

    private static final List<String> KEYS = List.of("j/k move", "h/l panel", "q quit");

    @Test
    void shouldShowEveryKeyWhenTheyAllFit() {
        assertThat(Screen.of(new KeyBarElement(KEYS), 40, 1).lines().getFirst().stripTrailing())
                .isEqualTo(" j/k move  h/l panel  q quit");
    }

    @Test
    void shouldDropTheLastKeysWholeRatherThanCutOneInHalf() {
        assertThat(Screen.of(new KeyBarElement(KEYS), 25, 1).lines().getFirst().stripTrailing())
                .isEqualTo(" j/k move  h/l panel");
    }

    @Test
    void shouldNotShowALaterKeyThatFitsWhenAnEarlierOneDoesNot() {
        assertThat(Screen.of(new KeyBarElement(List.of("j/k move", "alt+h/j/k/l focus", "q")), 20, 1).lines().getFirst().stripTrailing())
                .as("the keys are in order of importance, so a gap would show a minor key in place of a major one")
                .isEqualTo(" j/k move");
    }

    @Test
    void shouldCutTheFirstKeyWithAnEllipsisWhenEvenItIsTooWide() {
        assertThat(Screen.of(new KeyBarElement(KEYS), 6, 1).lines().getFirst().stripTrailing()).isEqualTo(" j/k …");
    }

    @Test
    void shouldDrawTheKeysDim() {
        assertThat(Screen.of(new KeyBarElement(KEYS), 40, 1).dimText()).anySatisfy(text -> assertThat(text).contains("q quit"));
    }
}
