package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.keybindingsJson;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ConfigYmlFixture.keybindingsJsonLinkingToNothing;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.altZ;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.chord;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.ctrlAltH;
import static dev.maxenceng.lazyclaude.session.published.ChordFixture.namedChord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.maxenceng.lazyclaude.session.domain.ClaudeKeybindingsUnreadableException;
import dev.maxenceng.lazyclaude.session.published.Modifier;
import dev.maxenceng.lazyclaude.session.published.NamedKey;

class ClaudeKeybindingsRepositoryTest {

    @TempDir
    Path home;

    private ClaudeKeybindingsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ClaudeKeybindingsRepository(home);
    }

    @Test
    void shouldSayNothingWhenThereIsNoFile() {
        assertThat(repository.bindings()).isEmpty();
    }

    @Test
    void shouldReadTheBindingsOfEveryContext() throws IOException {
        keybindingsJson(home, """
                {"bindings": [
                  {"context": "Chat", "bindings": {"alt+h": "chat:cancel"}},
                  {"context": "Global", "bindings": {"alt+z": "app:redraw"}}
                ]}""");

        assertThat(repository.bindings()).containsExactlyInAnyOrder(altH(), altZ());
    }

    @Test
    void shouldReadMetaAndOptionAsAltAndIgnoreCase() throws IOException {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"Meta+H": "chat:cancel", "OPTION+z": "app:redraw"}}]}""");

        assertThat(repository.bindings()).containsExactlyInAnyOrder(altH(), altZ());
    }

    @Test
    void shouldNotCountANullBindingAsOne() throws IOException {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"alt+h": null, "ctrl+alt+h": "chat:cancel"}}]}""");

        assertThat(repository.bindings()).containsExactly(ctrlAltH());
    }

    @Test
    void shouldTakeTheFirstKeystrokeOfASequence() throws IOException {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"alt+h ctrl+k": "chat:killAgents"}}]}""");

        assertThat(repository.bindings()).containsExactly(altH());
    }

    @Test
    void shouldSkipBindingsNoReservedChordCouldBe() throws IOException {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"escape": "chat:cancel", "cmd+k": "chat:clearScreen"}}]}""");

        assertThat(repository.bindings()).isEmpty();
    }

    @Test
    void shouldReadClaudesOtherNamesForEscapeEnterAndSpace() throws IOException {
        keybindingsJson(home, """
                {"bindings": [{"context": "Chat", "bindings": {"alt+esc": "chat:cancel", "ctrl+Return": "chat:submit", "ctrl+space": "chat:stash"}}]}""");

        assertThat(repository.bindings()).containsExactlyInAnyOrder(
                namedChord(NamedKey.ESCAPE, Modifier.ALT), namedChord(NamedKey.ENTER, Modifier.CTRL), chord(' ', Modifier.CTRL));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "[]",
        "{\"$schema\": \"x\"}",
        "{\"bindings\": \"alt+h\"}",
        "{\"bindings\": [\"alt+h\"]}",
        "{\"bindings\": [{\"context\": \"Chat\"}]}",
        "{\"bindings\": {\"Chat\": {\"bindings\": {\"alt+h\": \"chat:cancel\"}}}}",
        "{\"bindings\": [{\"context\": \"Chat\", \"bindings\": {\"alt+h\": 1}}]}"
    })
    void shouldSayAFileNotOfTheShapeClaudeWritesCannotBeRead(String text) throws IOException {
        keybindingsJson(home, text);

        assertThatThrownBy(repository::bindings)
                .as("a check that read no binding from it would report no clash it had not looked for")
                .isInstanceOf(ClaudeKeybindingsUnreadableException.class);
    }

    @Test
    void shouldSayWhichLineAFileThatIsNotJsonBreaksOn() throws IOException {
        keybindingsJson(home, "{\n  \"bindings\": [,\n}");

        assertThatThrownBy(repository::bindings).hasMessageContaining("keybindings.json:2:");
    }

    @Test
    void shouldSayALinkToAFileThatDoesNotExistCannotBeRead() throws IOException {
        keybindingsJsonLinkingToNothing(home);

        assertThatThrownBy(repository::bindings)
                .isInstanceOf(ClaudeKeybindingsUnreadableException.class)
                .hasMessageContaining("links to a file that does not exist");
    }

    @Test
    void shouldSayWhyAFileThatIsNotJsonCannotBeRead() throws IOException {
        keybindingsJson(home, "{ bindings: ");

        assertThatThrownBy(repository::bindings)
                .isInstanceOf(ClaudeKeybindingsUnreadableException.class)
                .hasMessageContaining("keybindings.json");
    }

    @Test
    void shouldSayWhyAFileItCannotOpenCannotBeRead() throws IOException {
        Files.createDirectories(home.resolve(".claude/keybindings.json"));

        assertThatThrownBy(repository::bindings).isInstanceOf(ClaudeKeybindingsUnreadableException.class);
    }
}
