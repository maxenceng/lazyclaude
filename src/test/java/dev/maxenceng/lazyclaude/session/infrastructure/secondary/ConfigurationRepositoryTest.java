package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.defaultConfiguration;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.rebindingFocusLeft;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withoutDesktopNotifications;
import static dev.maxenceng.lazyclaude.session.domain.ConfigurationFixture.withoutMouse;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYml;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYmlInLatin1;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.configYmlLinkingToNothing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.ConfigurationRefusedException;

class ConfigurationRepositoryTest {

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
    void shouldGiveTheDefaultsWhenThereIsNoFile() {
        assertThat(repository.configuration()).isEqualTo(defaultConfiguration());
    }

    @Test
    void shouldGiveTheDefaultsForAnEmptyFile() throws IOException {
        configYml(configHome, "");

        assertThat(repository.configuration()).isEqualTo(defaultConfiguration());
    }

    @Test
    void shouldRebindOneActionAndLeaveTheRestAtTheirDefault() throws IOException {
        configYml(configHome, "keybinding:", "  focusLeft: ctrl+y");

        assertThat(repository.configuration()).isEqualTo(rebindingFocusLeft());
    }

    @Test
    void shouldTurnDesktopNotificationsOff() throws IOException {
        configYml(configHome, "notifications:", "  desktop: false");

        assertThat(repository.configuration()).isEqualTo(withoutDesktopNotifications());
    }

    @Test
    void shouldTurnTheMouseOff() throws IOException {
        configYml(configHome, "mouse: false");

        assertThat(repository.configuration()).isEqualTo(withoutMouse());
    }

    @Test
    void shouldTurnTheMouseOffByDefaultOnTheLinuxConsole() {
        assertThat(new ConfigurationRepository(home, Map.of("TERM", "linux")).configuration()).isEqualTo(withoutMouse());
    }

    @Test
    void shouldLeaveTheMouseOnByDefaultOnAnyOtherTerminal() {
        assertThat(new ConfigurationRepository(home, Map.of("TERM", "xterm-256color")).configuration())
                .isEqualTo(defaultConfiguration());
    }

    @Test
    void shouldTurnTheMouseOnOnTheLinuxConsoleWhenTheFileAsksForIt() throws IOException {
        configYml(configHome, "mouse: true");

        assertThat(new ConfigurationRepository(home, Map.of("TERM", "linux")).configuration()).isEqualTo(defaultConfiguration());
    }

    @Test
    void shouldRefuseAMouseSettingThatIsNotABoolean() throws IOException {
        configYml(configHome, "notifications:", "  desktop: true", "mouse: sometimes");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:3: 'sometimes' is not a valid value for mouse: use true or false");
    }

    @Test
    void shouldRefuseAMouseSettingThatIsNotAScalar() throws IOException {
        configYml(configHome, "mouse:", "  capture: true");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:1: mouse needs true or false");
    }

    @Test
    void shouldReadTheFileUnderTheXdgConfigHomeWhenItIsSet() throws IOException {
        Path elsewhere = home.resolve("srv");
        configYml(elsewhere, "notifications:", "  desktop: false");

        assertThat(new ConfigurationRepository(home, Map.of("XDG_CONFIG_HOME", elsewhere.toString())).configuration())
                .isEqualTo(withoutDesktopNotifications());
    }

    @Test
    void shouldReadTheFileUnderTheHomeConfigDirectoryWhenTheXdgConfigHomeIsBlank() throws IOException {
        configYml(configHome, "notifications:", "  desktop: false");

        assertThat(new ConfigurationRepository(home, Map.of("XDG_CONFIG_HOME", " ")).configuration())
                .isEqualTo(withoutDesktopNotifications());
    }

    @Test
    void shouldRefuseAnUnknownKeyNamingTheFileTheLineAndTheKey() throws IOException {
        configYml(configHome, "keybinding:", "  focusDown: alt+j", "  focusLeftt: alt+h");

        assertThatThrownBy(repository::configuration)
                .isInstanceOf(ConfigurationRefusedException.class)
                .hasMessage("~/.config/lazyclaude/config.yml:3: unknown key 'focusLeftt'");
    }

    @Test
    void shouldRefuseAnUnknownTopLevelKey() throws IOException {
        configYml(configHome, "keybindings:", "  zoom: alt+z");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:1: unknown key 'keybindings'");
    }

    @Test
    void shouldRefuseAnUnknownNotificationKey() throws IOException {
        configYml(configHome, "notifications:", "  mobile: true");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:2: unknown key 'mobile'");
    }

    @Test
    void shouldRefuseANotificationSettingThatIsNotABoolean() throws IOException {
        configYml(configHome, "notifications:", "  desktop: no");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:2: 'no' is not a valid value for desktop: use true or false");
    }

    @Test
    void shouldRefuseAChordWithNoModifier() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: z");

        assertThatThrownBy(repository::configuration).hasMessageStartingWith("~/.config/lazyclaude/config.yml:2: ").hasMessageContaining("no modifier");
    }

    @Test
    void shouldRefuseAChordOfShiftAlone() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: shift+z");

        assertThatThrownBy(repository::configuration).hasMessageStartingWith("~/.config/lazyclaude/config.yml:2: ").hasMessageContaining("shift alone");
    }

    @Test
    void shouldRefuseAModifierOtherThanCtrlAltOrShift() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: alt+z", "  hideMainView: meta+m");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:3: 'meta' is not a modifier: use ctrl, alt or shift");
    }

    @Test
    void shouldRefuseASequenceOfKeystrokes() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: ctrl+x ctrl+z");

        assertThatThrownBy(repository::configuration).hasMessageStartingWith("~/.config/lazyclaude/config.yml:2: ").hasMessageContaining("several keystrokes");
    }

    @Test
    void shouldRefuseAChordThatIsNotText() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: [alt, z]");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:2: zoom needs a chord such as alt+z");
    }

    @Test
    void shouldRefuseAnEmptyChord() throws IOException {
        configYml(configHome, "keybinding:", "  zoom:");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:2: zoom needs a chord such as alt+z");
    }

    @Test
    void shouldRefuseAChordAnotherActionHoldsByDefaultAtTheLineThatAsksForIt() throws IOException {
        configYml(configHome, "keybinding:", "  focusLeft: alt+j");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:2: alt+j is already bound to focusDown by default");
    }

    @Test
    void shouldRefuseTwoActionsOnOneChordAtTheSecondWithTheFirstsLine() throws IOException {
        configYml(configHome, "keybinding:", "  focusLeft: ctrl+y", "  zoom: alt+z", "  focusDown: ctrl+y");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:4: ctrl+y is already bound to focusLeft on line 2");
    }

    @Test
    void shouldRefuseAChordTheTerminalNeverDeliversAtItsLine() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: alt+z", "  focusLeft: ctrl+alt+h");

        assertThatThrownBy(repository::configuration)
                .isInstanceOf(ConfigurationRefusedException.class)
                .hasMessage("~/.config/lazyclaude/config.yml:3: 'ctrl+alt+h' never reaches lazyclaude:"
                        + " the terminal cannot report ctrl and alt together on a character");
    }

    @Test
    void shouldRefuseOneOfLazyclaudesFixedChordsAtTheLineThatAsksForIt() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: alt+z", "  hideMainView: ctrl+c");

        assertThatThrownBy(repository::configuration)
                .isInstanceOf(ConfigurationRefusedException.class)
                .hasMessage("~/.config/lazyclaude/config.yml:3: ctrl+c is lazyclaude's quit");
    }

    @Test
    void shouldAllowTwoActionsToSwapTheirDefaults() throws IOException {
        configYml(configHome, "keybinding:", "  focusLeft: alt+j", "  focusDown: alt+h");

        assertThat(repository.configuration().reservedChords().of(dev.maxenceng.lazyclaude.session.published.ReservedAction.FOCUS_LEFT).text())
                .isEqualTo("alt+j");
    }

    @Test
    void shouldRefuseAKeyWrittenTwice() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: alt+z", "  zoom: alt+y");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:3: duplicate key 'zoom'");
    }

    @Test
    void shouldRefuseTextThatIsNotYamlAtTheLineItBreaksOn() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: [alt+z");

        assertThatThrownBy(repository::configuration).hasMessageStartingWith("~/.config/lazyclaude/config.yml:");
    }

    @Test
    void shouldRefuseAFileThatIsNotAMapping() throws IOException {
        configYml(configHome, "- alt+z");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:1: expected entries such as keybinding or notifications");
    }

    @Test
    void shouldRefuseAFileItCannotRead() throws IOException {
        Files.createDirectories(configHome.resolve("lazyclaude/config.yml"));

        assertThatThrownBy(repository::configuration)
                .isInstanceOf(ConfigurationRefusedException.class)
                .hasMessageStartingWith("~/.config/lazyclaude/config.yml: cannot read");
    }

    @Test
    void shouldRefuseALinkToAFileThatDoesNotExist() throws IOException {
        configYmlLinkingToNothing(configHome);

        assertThatThrownBy(repository::configuration)
                .as("the rebindings the link was meant to bring would quietly not apply")
                .hasMessage("~/.config/lazyclaude/config.yml: links to a file that does not exist");
    }

    @Test
    void shouldRefuseAFileThatIsNotUtf8() throws IOException {
        configYmlInLatin1(configHome);

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml: cannot read: not UTF-8 text");
    }

    @Test
    void shouldRefuseACharacterYamlDoesNotAllowAtItsLine() throws IOException {
        configYml(configHome, "keybinding:", "  zoom: alt+z", "  hideMainView: alt+\u0001");

        assertThatThrownBy(repository::configuration)
                .hasMessage("~/.config/lazyclaude/config.yml:3: character U+0001 is not allowed");
    }

    @Test
    void shouldRefuseAKeyThatIsNotAName() throws IOException {
        configYml(configHome, "keybinding:", "  ? [zoom]", "  : alt+z");

        assertThatThrownBy(repository::configuration).hasMessage("~/.config/lazyclaude/config.yml:2: a key must be a name");
    }
}
