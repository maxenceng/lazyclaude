package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocumentAt;
import static dev.maxenceng.lazyclaude.docs.domain.UnreadableDocumentFixture.unreadableDocument;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.adrPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.deepPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.researchPath;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondAdrPath;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.adr;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.research;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.superseded;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

class DocsTreeTest {

    @Test
    void shouldBeEmptyWhenThereAreNoDocuments() {
        DocsTree tree = DocsTree.of(List.of());

        assertThat(tree.root().documents()).isEmpty();
        assertThat(tree.root().folders()).isEmpty();
    }

    @Test
    void shouldListTheFilesDirectlyUnderDocsAtTheRootByName() {
        DocsTree tree = DocsTree.of(List.of(unreadableDocument(), readDocument()));

        assertThat(tree.root().documents())
                .extracting(Document::path)
                .containsExactly(readDocument().path(), unreadableDocument().path());
    }

    @Test
    void shouldGroupDocumentsUnderTheirFolderSortedByName() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(secondAdrPath()), readDocumentAt(adrPath())));

        assertThat(tree.root().documents()).isEmpty();
        assertThat(tree.root().folders()).singleElement().satisfies(folder -> {
            assertThat(folder.name()).isEqualTo(adr());
            assertThat(folder.documents())
                    .extracting(Document::path)
                    .containsExactly(adrPath(), secondAdrPath());
        });
    }

    @Test
    void shouldSortFoldersByName() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(researchPath()), readDocumentAt(adrPath())));

        assertThat(tree.root().folders()).extracting(Folder::name).containsExactly(adr(), research());
    }

    @Test
    void shouldNestFoldersAnyDepth() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(deepPath()), readDocumentAt(adrPath())));

        Folder adrFolder = tree.root().folders().getFirst();
        assertThat(adrFolder.documents()).extracting(Document::path).containsExactly(adrPath());
        assertThat(adrFolder.folders()).singleElement().satisfies(folder -> {
            assertThat(folder.name()).isEqualTo(superseded());
            assertThat(folder.documents()).extracting(Document::path).containsExactly(deepPath());
        });
    }

    @Test
    void shouldPlaceFilesBeforeFoldersAtEveryLevel() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(adrPath()), readDocument()));

        assertThat(tree.root().documents()).extracting(Document::path).containsExactly(readDocument().path());
        assertThat(tree.root().folders()).hasSize(1);
    }

    @Test
    void shouldKeepAnUnreadableDocumentInItsFolder() {
        Document unreadable = UnreadableDocument.builder()
                .path(adrPath())
                .reason(unreadableDocument().reason())
                .build();

        assertThat(DocsTree.of(List.of(unreadable)).root().folders().getFirst().documents())
                .containsExactly(unreadable);
    }

    @Test
    void shouldLeaveOutDocumentsThatAreNotListed() {
        DocumentPath backlog = new DocumentPath("backlog/DOCS-1.md");
        DocumentPath image = new DocumentPath("adr/diagram.png");

        DocsTree tree = DocsTree.of(List.of(readDocumentAt(backlog), readDocumentAt(image)));

        assertThat(tree.root().documents()).isEmpty();
        assertThat(tree.root().folders()).as("a folder with nothing listed is not a folder").isEmpty();
    }
}
