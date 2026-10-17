package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.addingIn;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.bothLines;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.line;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnLine;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.settingOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.KittyConfFixture.confFile;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.KittyConfFixture.confFileWithoutFinalNewline;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.KittyConfFixture.configFile;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.KittyConfFixture.textOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.session.domain.KittyConfigFile;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigKey;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigUnreadableException;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigUnwritableException;
import dev.maxenceng.lazyclaude.session.domain.KittySettings;

class KittyConfigRepositoryTest {

    private static final List<KittyConfigKey> KEYS = KittySettings.remoteControlKeys();

    @TempDir
    Path home;

    private Path kittyDirectory;

    @BeforeEach
    void setUp() {
        kittyDirectory = home.resolve(".config/kitty");
        repository = new KittyConfigRepository(home, Map.of());
    }

    private KittyConfigRepository repository;

    @Test
    void shouldLocateKittyConfUnderTheConfigDirectoryTheEnvironmentNames() {
        assertThat(new KittyConfigRepository(home, Map.of("KITTY_CONFIG_DIRECTORY", "/etc/kitty-here")).locate())
                .isEqualTo(new KittyConfigFile(Path.of("/etc/kitty-here/kitty.conf")));
    }

    @Test
    void shouldLocateKittyConfUnderTheXdgConfigHomeWhenNoKittyConfigDirectoryIsSet() {
        assertThat(new KittyConfigRepository(home, Map.of("XDG_CONFIG_HOME", "/srv/config")).locate())
                .as("kitty looks under $XDG_CONFIG_HOME before ~/.config, so editing ~/.config would change a file kitty never reads")
                .isEqualTo(new KittyConfigFile(Path.of("/srv/config/kitty/kitty.conf")));
    }

    @Test
    void shouldPreferTheKittyConfigDirectoryOverTheXdgConfigHome() {
        assertThat(new KittyConfigRepository(home, Map.of("KITTY_CONFIG_DIRECTORY", "/etc/kitty-here", "XDG_CONFIG_HOME", "/srv/config")).locate())
                .isEqualTo(new KittyConfigFile(Path.of("/etc/kitty-here/kitty.conf")));
    }

    @Test
    void shouldIgnoreABlankXdgConfigHome() {
        assertThat(new KittyConfigRepository(home, Map.of("XDG_CONFIG_HOME", " ")).locate()).isEqualTo(configFile(kittyDirectory, "kitty.conf"));
    }

    @Test
    void shouldLocateKittyConfUnderTheHomeConfigDirectoryWhenTheEnvironmentNamesNone() {
        assertThat(repository.locate()).isEqualTo(configFile(kittyDirectory, "kitty.conf"));
    }

    @Test
    void shouldIgnoreABlankConfigDirectoryInTheEnvironment() {
        assertThat(new KittyConfigRepository(home, Map.of("KITTY_CONFIG_DIRECTORY", " ")).locate()).isEqualTo(configFile(kittyDirectory, "kitty.conf"));
    }

    @Test
    void shouldReadTheSettingsOfTheKeysAskedForWithTheirLineNumbers() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "font_size 12", "# allow_remote_control no", "", "allow_remote_control yes", "listen_on\tunix:/tmp/kitty  ");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(
                        settingOf(line("allow_remote_control", "yes"), configFile(kittyDirectory, "kitty.conf"), 4),
                        settingOf(line("listen_on", "unix:/tmp/kitty"), configFile(kittyDirectory, "kitty.conf"), 5));
    }

    @Test
    void shouldMakeNoSettingsFromAFileThatDoesNotExist() {
        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found()).isEmpty();
    }

    @Test
    void shouldSkipAKeyWithNoValue() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "allow_remote_control");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found()).isEmpty();
    }

    @Test
    void shouldReadAnIncludedFileWhereTheIncludeIsWritten() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "allow_remote_control no", "include user.conf", "listen_on unix:/tmp/main");
        confFile(kittyDirectory, "user.conf", "allow_remote_control yes");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .as("kitty applies a file's lines in order, an included file's in place, so the last one wins")
                .containsExactly(
                        settingOf(line("allow_remote_control", "no"), configFile(kittyDirectory, "kitty.conf"), 1),
                        settingOf(line("allow_remote_control", "yes"), configFile(kittyDirectory, "user.conf"), 1),
                        settingOf(line("listen_on", "unix:/tmp/main"), configFile(kittyDirectory, "kitty.conf"), 3));
    }

    @Test
    void shouldFollowAnIncludeGivenByAbsolutePathAndByHomeRelativePath() throws IOException {
        Path elsewhere = confFile(home, "elsewhere/extra.conf", "listen_on unix:@one");
        confFile(home, ".config/kitty/kitty.conf", "include " + elsewhere, "include ~/elsewhere/extra.conf");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .extracting(setting -> setting.place().file().path())
                .containsExactly(elsewhere, elsewhere);
    }

    @Test
    void shouldReadAKeySetInAGlobincludedFile() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "globinclude conf.d/*.conf");
        confFile(kittyDirectory, "conf.d/b.conf", "listen_on unix:@b");
        confFile(kittyDirectory, "conf.d/a.conf", "allow_remote_control yes");
        confFile(kittyDirectory, "conf.d/ignored.txt", "allow_remote_control no");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(
                        settingOf(line("allow_remote_control", "yes"), configFile(kittyDirectory, "conf.d/a.conf"), 1),
                        settingOf(line("listen_on", "unix:@b"), configFile(kittyDirectory, "conf.d/b.conf"), 1));
    }

    @Test
    void shouldReadAGlobincludeThatMatchesNothingAsNoSettings() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "globinclude conf.d/*.conf", "allow_remote_control yes");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(settingOf(line("allow_remote_control", "yes"), configFile(kittyDirectory, "kitty.conf"), 2));
    }

    @Test
    void shouldReadARecursiveGlobinclude() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "globinclude themes/**/*.conf");
        confFile(kittyDirectory, "themes/dark/deep/x.conf", "listen_on unix:@deep");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(settingOf(line("listen_on", "unix:@deep"), configFile(kittyDirectory, "themes/dark/deep/x.conf"), 1));
    }

    @Test
    void shouldReadARecursiveGlobincludeThatMatchesNoDirectoryInBetween() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "globinclude themes/**/*.conf");
        confFile(kittyDirectory, "themes/x.conf", "allow_remote_control no");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .as("kitty's ** matches zero directories too, as pathlib's does")
                .containsExactly(settingOf(line("allow_remote_control", "no"), configFile(kittyDirectory, "themes/x.conf"), 1));
    }

    @Test
    void shouldExpandTheEnvironmentInAnIncludedPath() throws IOException {
        Path elsewhere = confFile(home, "elsewhere/extra.conf", "listen_on unix:@one");
        confFile(kittyDirectory, "kitty.conf", "include ${EXTRA}/extra.conf", "include $EXTRA/extra.conf");
        KittyConfigRepository expanding =
                new KittyConfigRepository(home, Map.of("EXTRA", home.resolve("elsewhere").toString()));

        assertThat(expanding.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .extracting(setting -> setting.place().file().path())
                .containsExactly(elsewhere, elsewhere);
    }

    @Test
    void shouldNotFollowASymlinkedDirectoryBackIntoTheFileForever() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "include sub/kitty.conf", "listen_on unix:@main");
        Files.createSymbolicLink(kittyDirectory.resolve("sub"), kittyDirectory);

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(settingOf(line("listen_on", "unix:@main"), configFile(kittyDirectory, "kitty.conf"), 2));
    }

    @Test
    void shouldSkipAnIncludedFileThatDoesNotExist() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "include missing.conf", "listen_on unix:@here");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(settingOf(line("listen_on", "unix:@here"), configFile(kittyDirectory, "kitty.conf"), 2));
    }

    @Test
    void shouldNotFollowAnIncludeCycleForever() throws IOException {
        confFile(kittyDirectory, "kitty.conf", "include other.conf", "listen_on unix:@main");
        confFile(kittyDirectory, "other.conf", "include kitty.conf", "allow_remote_control yes");

        assertThat(repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS).found())
                .containsExactly(
                        settingOf(line("allow_remote_control", "yes"), configFile(kittyDirectory, "other.conf"), 2),
                        settingOf(line("listen_on", "unix:@main"), configFile(kittyDirectory, "kitty.conf"), 2));
    }

    @Test
    void shouldSayWhyAConfigThatIsThereCannotBeRead() throws IOException {
        Files.createDirectories(kittyDirectory.resolve("kitty.conf"));

        assertThatThrownBy(() -> repository.settings(configFile(kittyDirectory, "kitty.conf"), KEYS))
                .isInstanceOf(KittyConfigUnreadableException.class)
                .hasMessageContaining("kitty.conf");
    }

    @Test
    void shouldAppendTheLinesUnderACommentSayingLazyclaudeAddedThem() throws IOException {
        Path conf = confFile(kittyDirectory, "kitty.conf", "font_size 12");

        repository.append(addingIn(configFile(kittyDirectory, "kitty.conf"), bothLines()));

        assertThat(textOf(conf))
                .isEqualTo("font_size 12\n\n# added by lazyclaude\nallow_remote_control socket-only\nlisten_on unix:@kitty-{kitty_pid}\n");
    }

    @Test
    void shouldFinishALastLineThatHasNoNewlineBeforeAppending() throws IOException {
        Path conf = confFileWithoutFinalNewline(kittyDirectory, "kitty.conf", "font_size 12");

        repository.append(addingIn(configFile(kittyDirectory, "kitty.conf"), List.of(listenOnLine())));

        assertThat(textOf(conf)).isEqualTo("font_size 12\n\n# added by lazyclaude\nlisten_on unix:@kitty-{kitty_pid}\n");
    }

    @Test
    void shouldCreateTheFileAndItsDirectoryWhenThereIsNone() throws IOException {
        repository.append(addingIn(configFile(kittyDirectory, "kitty.conf"), bothLines()));

        assertThat(textOf(kittyDirectory.resolve("kitty.conf")))
                .isEqualTo("# added by lazyclaude\nallow_remote_control socket-only\nlisten_on unix:@kitty-{kitty_pid}\n");
    }

    @Test
    void shouldSayWhyTheLinesCannotBeAppended() throws IOException {
        Files.createDirectories(home.resolve(".config"));
        Files.writeString(kittyDirectory, "a file where the directory should be");

        assertThatThrownBy(() -> repository.append(addingIn(configFile(kittyDirectory, "kitty.conf"), bothLines())))
                .isInstanceOf(KittyConfigUnwritableException.class)
                .hasMessageContaining("kitty.conf");
    }
}
