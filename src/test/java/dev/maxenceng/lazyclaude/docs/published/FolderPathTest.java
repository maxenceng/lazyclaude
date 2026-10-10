package dev.maxenceng.lazyclaude.docs.published;

import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.superseded;
import static dev.maxenceng.lazyclaude.docs.published.FolderPathFixture.supersededFolderPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class FolderPathTest {

    @Test
    void shouldRefuseAMissingPath() {
        assertThatThrownBy(() -> new FolderPath(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseAPathThatClimbsOutOfDocs() {
        assertThatThrownBy(() -> new FolderPath("adr/.."))
                .as("a folder path is spelled the way a document path is")
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldJoinItsSegments() {
        assertThat(FolderPath.of(List.of("adr", "superseded"))).isEqualTo(supersededFolderPath());
    }

    @Test
    void shouldBeNamedAfterItsLastSegment() {
        assertThat(supersededFolderPath().name()).isEqualTo(superseded());
    }
}
