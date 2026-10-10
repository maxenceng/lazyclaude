package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.createdEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.idEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class FrontmatterTest {

    @Test
    void shouldKeepItsEntriesInFileOrder() {
        assertThat(new Frontmatter(List.of(createdEntry(), idEntry())).entries())
                .as("file order, not alphabetical")
                .containsExactly(createdEntry(), idEntry());
    }

    @Test
    void shouldRefuseMissingEntries() {
        assertThatThrownBy(() -> new Frontmatter(null)).isInstanceOf(MissingMandatoryValueException.class);
    }
}
