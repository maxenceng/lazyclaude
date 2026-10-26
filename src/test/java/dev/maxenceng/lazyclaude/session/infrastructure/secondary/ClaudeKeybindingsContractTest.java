package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsDocsFixture.defaultBindingsInTheDocumentation;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.ClaudeKeybindingsDocsFixture.rowsInTheDocumentation;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

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
    void shouldFindNoDefaultBindingOnAReservedChord() throws IOException, InterruptedException {
        assertThat(ReservedChords.defaults().clashingWith(defaultBindingsInTheDocumentation()))
                .as("a default binding of Claude Code on a chord lazyclaude reserves would be shadowed inside every hosted session")
                .isEmpty();
    }
}
