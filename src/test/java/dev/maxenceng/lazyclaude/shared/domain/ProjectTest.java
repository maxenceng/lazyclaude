package dev.maxenceng.lazyclaude.shared.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void shouldRefuseARelativeRoot() {
        assertThatThrownBy(() -> new Project(Path.of("app")))
                .as("two relative roots from different directories would compare equal")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
