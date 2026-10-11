package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.noTitle;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class SessionTitleTest {

    @Test
    void shouldPreferTheAiTitleOverTheFirstPrompt() {
        assertThat(SessionTitle.of(Optional.of(aiTitle().value()), Optional.of(firstPrompt().value()))).isEqualTo(aiTitle());
    }

    @Test
    void shouldFallBackToTheFirstPromptWithoutAnAiTitle() {
        assertThat(SessionTitle.of(Optional.empty(), Optional.of(firstPrompt().value()))).isEqualTo(firstPrompt());
    }

    @Test
    void shouldHaveNoTitleWithNeither() {
        assertThat(SessionTitle.of(Optional.empty(), Optional.empty())).isEqualTo(noTitle());
    }
}
