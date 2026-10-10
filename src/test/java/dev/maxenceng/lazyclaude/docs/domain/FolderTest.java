package dev.maxenceng.lazyclaude.docs.domain;

import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocument;
import static dev.maxenceng.lazyclaude.docs.domain.ReadDocumentFixture.readDocumentAt;
import static dev.maxenceng.lazyclaude.docs.published.DocumentPathFixture.secondDocumentPath;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.adr;
import static dev.maxenceng.lazyclaude.docs.published.FolderNameFixture.research;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class FolderTest {

    @Test
    void shouldSortWhatItIsGivenByName() {
        Folder folder = new Folder(
                adr(),
                List.of(readDocumentAt(secondDocumentPath()), readDocument()),
                List.of(new Folder(research(), List.of(), List.of()), new Folder(adr(), List.of(), List.of())));

        assertThat(folder.documents())
                .as("files sorted by name is the docs tree's rule, not only its factory's")
                .extracting(Document::path)
                .containsExactly(readDocument().path(), secondDocumentPath());
        assertThat(folder.folders()).extracting(Folder::name).containsExactly(adr(), research());
    }
}
