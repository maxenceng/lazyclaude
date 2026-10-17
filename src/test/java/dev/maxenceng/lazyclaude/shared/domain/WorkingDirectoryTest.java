package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectoryAt;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class WorkingDirectoryTest {

    @Test
    void shouldMakeARelativePathAbsolute() {
        Path path = new WorkingDirectory(Path.of("app")).path();

        assertThat(path.isAbsolute()).as("a project is located from where lazyclaude runs, not from a relative name").isTrue();
        assertThat(path.getFileName()).isEqualTo(Path.of("app"));
    }

    @Test
    void shouldNormaliseThePath() {
        assertThat(new WorkingDirectory(Path.of("/work/app/src/..")).path()).isEqualTo(Path.of("/work/app"));
    }

    @Test
    void shouldRefuseAMissingPath() {
        assertThatThrownBy(() -> new WorkingDirectory(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldContainADirectoryBelowIt() {
        assertThat(worktreeDirectory().contains(workingDirectoryAt("/work/app.worktrees/PROJ-1-ticket/src/main")))
                .isTrue();
    }

    @Test
    void shouldContainItself() {
        assertThat(worktreeDirectory().contains(worktreeDirectory())).isTrue();
    }

    @Test
    void shouldNotContainASiblingWhoseNameStartsWithItsName() {
        assertThat(worktreeDirectory().contains(workingDirectoryAt("/work/app.worktrees/PROJ-1-ticket-again/src")))
                .as("a path is below another by its segments, not by the characters they begin with")
                .isFalse();
    }

    @Test
    void shouldNotContainItsParent() {
        assertThat(worktreeDirectory().contains(workingDirectoryAt("/work/app.worktrees"))).isFalse();
    }
}
