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
import static dev.maxenceng.lazyclaude.docs.published.FolderPathFixture.adrFolderPath;
import static dev.maxenceng.lazyclaude.docs.published.FolderPathFixture.supersededFolderPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;

class DocsTreeTest {

    @Test
    void shouldBeEmptyWhenThereAreNoDocuments() {
        DocsTree tree = DocsTree.of(List.of());

        assertThat(tree.documents()).isEmpty();
        assertThat(tree.folders()).isEmpty();
    }

    @Test
    void shouldKeepTheFilesDirectlyUnderDocsInTheOrderTheyWereListed() {
        DocsTree tree = DocsTree.of(List.of(unreadableDocument(), readDocument()));

        assertThat(tree.documents())
                .as("ordering is the adapter's, before the domain")
                .extracting(Document::path)
                .containsExactly(unreadableDocument().path(), readDocument().path());
    }

    @Test
    void shouldGroupDocumentsUnderTheirFolder() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(adrPath()), readDocumentAt(secondAdrPath())));

        assertThat(tree.documents()).isEmpty();
        assertThat(tree.folders()).singleElement().satisfies(folder -> {
            assertThat(folder.path()).isEqualTo(adrFolderPath());
            assertThat(folder.name()).isEqualTo(adr());
            assertThat(folder.documents()).extracting(Document::path).containsExactly(adrPath(), secondAdrPath());
        });
    }

    @Test
    void shouldPlaceAFolderWhereItsFirstDocumentWasListed() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(researchPath()), readDocumentAt(adrPath())));

        assertThat(tree.folders()).extracting(Folder::name).containsExactly(research(), adr());
    }

    @Test
    void shouldNestFoldersAnyDepthEachKnownByItsPath() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(adrPath()), readDocumentAt(deepPath())));

        Folder adrFolder = tree.folders().getFirst();
        assertThat(adrFolder.documents()).extracting(Document::path).containsExactly(adrPath());
        assertThat(adrFolder.folders()).singleElement().satisfies(folder -> {
            assertThat(folder.path()).isEqualTo(supersededFolderPath());
            assertThat(folder.documents()).extracting(Document::path).containsExactly(deepPath());
        });
    }

    @Test
    void shouldHoldAFoldersOwnFilesApartFromItsFolders() {
        DocsTree tree = DocsTree.of(List.of(readDocumentAt(adrPath()), readDocument()));

        assertThat(tree.documents()).extracting(Document::path).containsExactly(readDocument().path());
        assertThat(tree.folders()).hasSize(1);
    }

    @Test
    void shouldKeepAnUnreadableDocumentInItsFolder() {
        Document unreadable = UnreadableDocument.builder()
                .path(adrPath())
                .reason(unreadableDocument().reason())
                .build();

        assertThat(DocsTree.of(List.of(unreadable)).folders().getFirst().documents()).containsExactly(unreadable);
    }

    @Test
    void shouldLeaveOutDocumentsThatAreNotListed() {
        DocumentPath backlog = new DocumentPath("backlog/ticket.md");
        DocumentPath image = new DocumentPath("adr/diagram.png");

        DocsTree tree = DocsTree.of(List.of(readDocumentAt(backlog), readDocumentAt(image)));

        assertThat(tree.documents()).isEmpty();
        assertThat(tree.folders()).as("a folder with nothing listed is not a folder").isEmpty();
    }
}
