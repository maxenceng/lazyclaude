package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.defaultConfiguration;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withInProgressDesignFirst;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withInReviewVerbs;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withTodoWithoutVerbs;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;

class ConfigurationRepositoryVerbsTest {

    private static final String FILE = "~/.config/lazyclaude/config.yml";

    @TempDir
    Path home;

    private Path configHome;
    private ConfigurationRepository repository;

    @BeforeEach
    void setUp() {
        configHome = home.resolve(".config");
        repository = new ConfigurationRepository(home, Map.of());
    }

    @Test
    void shouldGiveEveryStatusItsDefaultVerbsWhenTheFileHasNoVerbsSection() throws IOException {
        configYml(configHome, "mouse: true");

        assertThat(repository.configuration()).isEqualTo(defaultConfiguration());
    }

    @Test
    void shouldReadAFlowListInTheOrderItIsWritten() throws IOException {
        configYml(configHome, "verbs:", "  in-review: [done, respond]");

        assertThat(repository.configuration()).isEqualTo(withInReviewVerbs());
    }

    @Test
    void shouldReadABlockListInTheOrderItIsWritten() throws IOException {
        configYml(configHome, "verbs:", "  in-progress:", "    - design", "    - review");

        assertThat(repository.configuration()).isEqualTo(withInProgressDesignFirst());
    }

    @Test
    void shouldLeaveAStatusNoVerbWhenItsListIsEmpty() throws IOException {
        configYml(configHome, "verbs:", "  todo: []");

        assertThat(repository.configuration()).isEqualTo(withTodoWithoutVerbs());
    }

    @Test
    void shouldRefuseAStatusLabelItDoesNotKnow() throws IOException {
        configYml(configHome, "verbs:", "  blocked: [done]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: unknown key 'blocked'");
    }

    @Test
    void shouldRefuseAVerbItDoesNotKnow() throws IOException {
        configYml(configHome, "verbs:", "  in-review: [respond, ship]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: unknown verb 'ship': use refine, start, review, design, respond or done");
    }

    @Test
    void shouldRefuseAVerbListedTwiceUnderOneStatus() throws IOException {
        configYml(configHome, "verbs:", "  in-review:", "    - respond", "    - done", "    - respond");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":5: respond is listed twice under in-review");
    }

    @Test
    void shouldRefuseNewUnderAnyStatus() throws IOException {
        configYml(configHome, "verbs:", "  draft: [new]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: new is not a verb a status offers: it creates a ticket");
    }

    @Test
    void shouldRefuseRefineAnywhereButDraft() throws IOException {
        configYml(configHome, "verbs:", "  todo: [refine]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: refine is only offered under draft, not todo");
    }

    @Test
    void shouldRefuseStartAnywhereButTodo() throws IOException {
        configYml(configHome, "verbs:", "  draft: [start]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: start is only offered under todo, not draft");
    }

    @Test
    void shouldRefuseDesignAnywhereButInProgress() throws IOException {
        configYml(configHome, "verbs:", "  in-review: [design]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: design is only offered under in-progress, not in-review");
    }

    @Test
    void shouldRefuseAnyVerbUnderDone() throws IOException {
        configYml(configHome, "verbs:", "  done: [done]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: done is not offered under done: a finished ticket has no verb");
    }

    @Test
    void shouldRefuseAVerbsSectionThatIsNotEntries() throws IOException {
        configYml(configHome, "verbs: done");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":1: verbs needs entries of its own");
    }

    @Test
    void shouldRefuseAStatusWhoseVerbsAreNotAList() throws IOException {
        configYml(configHome, "verbs:", "  in-review: done");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: in-review needs a list of verbs such as [respond, done]");
    }

    @Test
    void shouldRefuseAVerbThatIsNotAName() throws IOException {
        configYml(configHome, "verbs:", "  in-review: [[done]]");

        assertThatThrownBy(repository::configuration)
                .isExactlyInstanceOf(ConfigurationRefusedException.class)
                .hasMessage(FILE + ":2: a verb must be a name");
    }
}
