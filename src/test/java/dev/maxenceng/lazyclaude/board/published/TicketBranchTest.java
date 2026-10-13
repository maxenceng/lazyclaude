package dev.maxenceng.lazyclaude.board.published;

import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.dottedFileName;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.doubleExtensionFileName;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.slugFileName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.dottedTicketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.slugTicketBranch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.BlankValueException;

class TicketBranchTest {

    @Test
    void shouldBeTheFileNameWithoutItsMarkdownExtension() {
        assertThat(TicketBranch.of(slugFileName())).isEqualTo(slugTicketBranch());
    }

    @Test
    void shouldKeepADotInsideTheName() {
        assertThat(TicketBranch.of(dottedFileName())).isEqualTo(dottedTicketBranch());
    }

    @Test
    void shouldDropTheExtensionOnlyOnce() {
        assertThat(TicketBranch.of(doubleExtensionFileName()).value()).isEqualTo("PROJ-1.md");
    }

    @Test
    void shouldRefuseABlankBranch() {
        assertThatThrownBy(() -> new TicketBranch(" ")).isInstanceOf(BlankValueException.class);
    }
}
