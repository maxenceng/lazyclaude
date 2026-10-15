package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.backendChanges;
import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.frontendChanges;
import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.noChanges;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BranchChangesTest {

    @Test
    void shouldTouchTheFrontendWhenAFileIsUnderFrontend() {
        assertThat(frontendChanges().touchesFrontend()).isTrue();
    }

    @Test
    void shouldNotTouchTheFrontendForAFileOnlyNamedAfterIt() {
        assertThat(backendChanges().touchesFrontend())
                .as("docs/frontend-notes.md is not under frontend/")
                .isFalse();
    }

    @Test
    void shouldNotTouchTheFrontendWithNoChangedFile() {
        assertThat(noChanges().touchesFrontend()).isFalse();
    }

    @Test
    void shouldNotTouchTheFrontendWhenTheChangesCannotBeRead() {
        assertThat(new UnreadChanges().touchesFrontend()).isFalse();
    }
}
