package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsDocsFixture.defaultBindingsInTheDocumentation;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsDocsFixture.rowsInTheDocumentation;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.ReservedChords;

@Tag("contract")
class ClaudeKeybindingsContractTest {

    @Test
    void shouldFindTheDefaultBindingsInTheDocumentationItReadsThemFrom() throws IOException, InterruptedException {
        assertThat(rowsInTheDocumentation())
                .as("the documentation's action tables changed shape, so this check would pass without checking anything")
                .isPositive();
        assertThat(defaultBindingsInTheDocumentation())
                .as("at least the Ctrl bindings of the documentation are read as chords")
                .isNotEmpty();
    }

    @Test
    void shouldListTheDefaultBindingsTheDocumentationLists() throws IOException, InterruptedException {
        assertThat(new ClaudeKeybindingsRepository(Path.of("")).defaults())
                .as("a default Claude Code added or dropped would be warned about wrongly until the list follows it")
                .containsExactlyInAnyOrderElementsOf(defaultBindingsInTheDocumentation().stream().distinct().toList());
    }

    @Test
    void shouldFindNoDefaultBindingOnAReservedChord() throws IOException, InterruptedException {
        assertThat(ReservedChords.defaults().clashingWith(defaultBindingsInTheDocumentation()))
                .as("a default binding of Claude Code on a chord lazyclaude reserves would be shadowed inside every hosted session")
                .isEmpty();
    }
}
