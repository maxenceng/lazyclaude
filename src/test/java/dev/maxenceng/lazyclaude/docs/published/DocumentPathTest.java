package dev.maxenceng.lazyclaude.docs.published;

import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.adrPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.deepPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.documentPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.error.domain.MissingMandatoryValueException;

class DocumentPathTest {

    @Test
    void shouldRefuseAMissingPath() {
        assertThatThrownBy(() -> new DocumentPath(null)).isInstanceOf(MissingMandatoryValueException.class);
    }

    @Test
    void shouldRefuseAPathWithAnEmptySegment() {
        assertThatThrownBy(() -> new DocumentPath("adr//a.md"))
                .as("a path is identity, so one spelling per file")
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRefuseAPathStartingWithASlash() {
        assertThatThrownBy(() -> new DocumentPath("/a.md")).isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRefuseAPathThatClimbsOutOfDocs() {
        assertThatThrownBy(() -> new DocumentPath("adr/../../secret.md"))
                .as("a document's place is under docs/, and reading it resolves this path")
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRefuseAPathWithACurrentFolderSegment() {
        assertThatThrownBy(() -> new DocumentPath("./a.md"))
                .as("a path is identity, so one spelling per file")
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldAcceptAFileNameStartingWithADot() {
        assertThat(new DocumentPath("adr/.draft.md").fileName()).isEqualTo(".draft.md");
    }

    @Test
    void shouldRefuseABlankPath() {
        assertThatThrownBy(() -> new DocumentPath(" ")).isInstanceOf(DomainException.class);
    }

    @Test
    void shouldSplitIntoItsSegments() {
        assertThat(deepPath().segments()).isEqualTo(List.of("adr", "superseded", "0000-old.md"));
    }

    @Test
    void shouldNameTheFileByItsLastSegment() {
        assertThat(adrPath().fileName()).isEqualTo("0001-record.md");
    }

    @Test
    void shouldBeListedWhenItIsAMarkdownFile() {
        assertThat(documentPath().listed()).isTrue();
        assertThat(deepPath().listed()).isTrue();
    }

    @Test
    void shouldNotBeListedWhenItIsNotMarkdown() {
        assertThat(new DocumentPath("openapi.json").listed()).isFalse();
        assertThat(new DocumentPath("adr/diagram.png").listed()).isFalse();
    }

    @Test
    void shouldNotBeListedWhenItIsUnderTheBacklog() {
        assertThat(new DocumentPath("backlog/DOCS-1.md").listed()).isFalse();
        assertThat(new DocumentPath("backlog/archive/DOCS-0.md").listed()).isFalse();
    }

    @Test
    void shouldBeListedWhenOnlyANestedFolderIsCalledBacklog() {
        assertThat(new DocumentPath("adr/backlog/a.md").listed())
                .as("only docs/backlog/ belongs to the board")
                .isTrue();
    }

    @Test
    void shouldBeListedWhenAFileIsNamedLikeTheBacklog() {
        assertThat(new DocumentPath("backlog.md").listed()).isTrue();
    }

    @Test
    void shouldOrderByPath() {
        assertThat(adrPath()).isLessThan(DocumentPathFixture.secondAdrPath());
    }
}
