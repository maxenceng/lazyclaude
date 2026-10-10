package dev.maxenceng.lazyclaude.shared.domain;

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
}
