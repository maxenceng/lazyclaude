package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.defaultConfigurationView;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYml;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.keybindingsJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.NoWarningView;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningView;

class ConfigurationLoaderTest {

    @TempDir
    Path home;

    private ConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ConfigurationLoader(home, Map.of());
    }

    @Test
    void shouldStartOnTheDefaultsWithoutAnyFile() {
        ConfigurationView configuration = loader.configuration();

        assertThat(configuration).isEqualTo(defaultConfigurationView());
        assertThat(loader.contexts(configuration).warnings().held()).isInstanceOf(NoWarningView.class);
    }

    @Test
    void shouldRefuseWithTheFileTheLineAndTheReasonWhenTheConfigurationIsRefused() throws Exception {
        configYml(home.resolve(".config"), "keybinding:", "  focusDown: alt+j", "  focusLeftt: alt+h");

        assertThatThrownBy(loader::configuration)
                .as("main prints the reason and opens no frontend")
                .isInstanceOf(DomainException.class)
                .hasMessage("~/.config/lazyclaude/config.yml:3: unknown key 'focusLeftt'");
    }

    @Test
    void shouldStartWithOneWarningListingEveryClashWithClaudesKeybindings() throws Exception {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"meta+z": "app:redraw", "ALT+H": "chat:cancel", "alt+m": null}}]}""");

        ConfigurationView configuration = loader.configuration();

        assertThat(loader.contexts(configuration).warnings().held())
                .as("the warnings raised while reading are held for the frontend that opens on these contexts")
                .isInstanceOfSatisfying(WarningView.class, warning -> {
                    assertThat(warning.count()).isEqualTo(1);
                    assertThat(warning.text()).contains("alt+z", "alt+h").doesNotContain("alt+m");
                });
    }

    @Test
    void shouldCarryOnWithAWarningWhenClaudesKeybindingsCannotBeRead() throws Exception {
        keybindingsJson(home, "not json");

        ConfigurationView configuration = loader.configuration();

        assertThat(loader.contexts(configuration).warnings().held())
                .isInstanceOfSatisfying(WarningView.class, warning -> {
                    assertThat(warning.count()).isEqualTo(1);
                    assertThat(warning.text()).contains("skipped");
                });
    }

    @Test
    void shouldKeepTheCacheUnderXdgCacheHomeWhenItIsSet() {
        assertThat(ConfigurationLoader.cacheHome(Path.of("/home/max"), "/var/cache/max")).isEqualTo(Path.of("/var/cache/max"));
    }

    @Test
    void shouldKeepTheCacheUnderTheDotCacheOfTheHomeWhenXdgCacheHomeIsUnsetOrBlank() {
        assertThat(ConfigurationLoader.cacheHome(Path.of("/home/max"), null)).isEqualTo(Path.of("/home/max/.cache"));
        assertThat(ConfigurationLoader.cacheHome(Path.of("/home/max"), "  ")).isEqualTo(Path.of("/home/max/.cache"));
    }

    @Test
    void shouldIgnoreARelativeXdgCacheHomeAsTheSpecDoes() {
        assertThat(ConfigurationLoader.cacheHome(Path.of("/home/max"), "cache")).isEqualTo(Path.of("/home/max/.cache"));
    }

    @Test
    void shouldKeepTheLaunchLogUnderXdgStateHomeWhenItIsSet() {
        assertThat(ConfigurationLoader.stateHome(Path.of("/home/max"), "/var/state/max")).isEqualTo(Path.of("/var/state/max"));
    }

    @Test
    void shouldKeepTheLaunchLogUnderDotLocalStateOfTheHomeWhenXdgStateHomeIsUnsetOrBlank() {
        assertThat(ConfigurationLoader.stateHome(Path.of("/home/max"), null)).isEqualTo(Path.of("/home/max/.local/state"));
        assertThat(ConfigurationLoader.stateHome(Path.of("/home/max"), "  ")).isEqualTo(Path.of("/home/max/.local/state"));
    }

    @Test
    void shouldIgnoreARelativeXdgStateHomeAsTheSpecDoes() {
        assertThat(ConfigurationLoader.stateHome(Path.of("/home/max"), "state")).isEqualTo(Path.of("/home/max/.local/state"));
    }
}
