package dev.maxenceng.lazyclaude;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationViewFixture.defaultConfigurationView;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYml;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.keybindingsJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.application.ConfigurationApplicationService;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.ConfigurationView;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsRepository;
import dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigurationRepository;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

class LazyClaudeConfigurationTest {

    @TempDir
    Path home;

    private final ByteArrayOutputStream said = new ByteArrayOutputStream();
    private final PrintStream err = new PrintStream(said, true, StandardCharsets.UTF_8);
    private final WarningRepository raised = new WarningRepository();
    private final List<String> warned = new ArrayList<>();
    private final List<ConfigurationView> opened = new ArrayList<>();
    private ConfigurationQuery query;

    @BeforeEach
    void setUp() {
        raised.subscribe(warnings -> {
            warned.clear();
            warned.addAll(warnings.raised().stream().map(Warning::text).toList());
        });
        query = new ConfigurationQuery(new ConfigurationApplicationService(
                new ConfigurationRepository(home, Map.of()), new ClaudeKeybindingsRepository(home), raised));
    }

    @Test
    void shouldStartOnTheDefaultsWithoutAnyFile() throws Exception {
        int exit = LazyClaude.configured(query, err, opened::add);

        assertThat(exit).isZero();
        assertThat(opened).containsExactly(defaultConfigurationView());
        assertThat(said.toString(StandardCharsets.UTF_8)).isEmpty();
        assertThat(warned).isEmpty();
    }

    @Test
    void shouldPrintTheFileTheLineAndTheReasonAndOpenNothingWhenTheConfigurationIsRefused() throws Exception {
        configYml(home.resolve(".config"), "keybinding:", "  focusDown: alt+j", "  focusLeftt: alt+h");

        int exit = LazyClaude.configured(query, err, opened::add);

        assertThat(exit).isEqualTo(1);
        assertThat(opened).as("no frame is drawn before the reason").isEmpty();
        assertThat(said.toString(StandardCharsets.UTF_8))
                .isEqualTo("lazyclaude: ~/.config/lazyclaude/config.yml:3: unknown key 'focusLeftt'%n".formatted());
    }

    @Test
    void shouldStartWithOneWarningListingEveryClashWithClaudesKeybindings() throws Exception {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"meta+z": "app:redraw", "ALT+H": "chat:cancel", "alt+m": null}}]}""");

        int exit = LazyClaude.configured(query, err, opened::add);

        assertThat(exit).isZero();
        assertThat(opened).hasSize(1);
        assertThat(warned).hasSize(1).allSatisfy(text -> assertThat(text).contains("alt+z", "alt+h").doesNotContain("alt+m"));
    }

    @Test
    void shouldCarryOnWithAWarningWhenClaudesKeybindingsCannotBeRead() throws Exception {
        keybindingsJson(home, "not json");

        assertThat(LazyClaude.configured(query, err, opened::add)).isZero();
        assertThat(opened).hasSize(1);
        assertThat(warned).hasSize(1).allSatisfy(text -> assertThat(text).contains("skipped"));
    }
}
