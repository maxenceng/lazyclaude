package dev.maxenceng.lazyclaude.session.published;

import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.noTitle;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReadFactsTest {

    @Test
    void shouldTitleByTheFirstPromptAndOpenWithItWithoutAnAiTitle() {
        assertThat(ReadFacts.of(Optional.empty(), Optional.of(firstPrompt().value())))
                .isEqualTo(new ReadFacts(firstPrompt(), firstPrompt()));
    }

    @Test
    void shouldKeepTheAiTitleAndOpenWithTheFirstPrompt() {
        assertThat(ReadFacts.of(Optional.of(aiTitle().value()), Optional.of(firstPrompt().value())))
                .isEqualTo(new ReadFacts(aiTitle(), firstPrompt()));
    }

    @Test
    void shouldHaveNeitherTitleNorOpeningWithNeither() {
        assertThat(ReadFacts.of(Optional.empty(), Optional.empty()))
                .isEqualTo(new ReadFacts(noTitle(), new NoFirstPrompt()));
    }
}
