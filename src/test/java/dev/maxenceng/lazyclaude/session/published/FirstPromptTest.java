package dev.maxenceng.lazyclaude.session.published;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.AssertionException;

class FirstPromptTest {

    @Test
    void shouldCutAMultiLinePromptToOneLine() {
        assertThat(new FirstPrompt("Fix the\n\n  status   line\tplease\n").value()).isEqualTo("Fix the status line please");
    }

    @Test
    void shouldKeepAPromptThatIsAlreadyOneLine() {
        assertThat(new FirstPrompt("/ticket start PROJ-1").value()).isEqualTo("/ticket start PROJ-1");
    }

    @Test
    void shouldRefuseABlankPrompt() {
        assertThatThrownBy(() -> new FirstPrompt(" \n ")).isInstanceOf(AssertionException.class);
    }
}
