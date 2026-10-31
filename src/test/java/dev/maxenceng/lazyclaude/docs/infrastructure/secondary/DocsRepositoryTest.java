package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.allow;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.churning;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.docsIn;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.forbid;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.markdown;
import static dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsFixture.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.maxenceng.lazyclaude.docs.domain.DocsUnavailableException;
import dev.maxenceng.lazyclaude.docs.domain.Document;
import dev.maxenceng.lazyclaude.docs.domain.ReadDocument;
import dev.maxenceng.lazyclaude.docs.domain.UnreadableDocument;
import dev.maxenceng.lazyclaude.docs.published.Content;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.EmptyDocument;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class DocsRepositoryTest {

    private final DocsRepository repository = new DocsRepository();

    @TempDir
    Path root;

    private Path docs;

    @BeforeEach
    void createDocs() throws IOException {
        docs = docsIn(root);
    }

    @Test
    void shouldListNoPathsWhenTheProjectHasNoDocsDirectory() throws IOException {
        Files.delete(docs);

        assertThat(repository.paths(new Project(root))).isEmpty();
    }

    @Test
    void shouldListThePathsWhileFilesComeAndGoBesideThem() throws Exception {
        write(docs, "glossary.md", markdown());
        Thread churn = churning(docs.resolve("backlog"));
        try {
            for (int read = 0; read < 300; read++) {
                assertThat(repository.paths(new Project(root)))
                        .as("a file deleted between listing a folder and looking at it is gone, not a failed read")
                        .contains(new DocumentPath("glossary.md"));
            }
        } finally {
            churn.interrupt();
            churn.join();
        }
    }

    @Test
    void shouldListEveryFileAtAnyDepthRelativeToDocs() throws IOException {
        write(docs, "glossary.md", markdown());
        write(docs, "adr/0001-record.md", markdown());
        write(docs, "adr/superseded/0000-old.md", markdown());

        assertThat(repository.paths(new Project(root)))
                .containsExactlyInAnyOrder(
                        new DocumentPath("glossary.md"),
                        new DocumentPath("adr/0001-record.md"),
                        new DocumentPath("adr/superseded/0000-old.md"));
    }

    @Test
    void shouldListThePathsInTreeOrder() throws IOException {
        write(docs, "glossary.md", markdown());
        write(docs, "adr-old/0000-first.md", markdown());
        write(docs, "adr/superseded/0000-old.md", markdown());
        write(docs, "adr/0001-record.md", markdown());
        write(docs, "context-map.md", markdown());
        write(docs, "adr/README.md", markdown());
        write(docs, "README.md", markdown());

        assertThat(repository.paths(new Project(root)))
                .as("ordering is done before the domain, which keeps it, and matches the site's")
                .containsExactly(
                        new DocumentPath("README.md"),
                        new DocumentPath("context-map.md"),
                        new DocumentPath("glossary.md"),
                        new DocumentPath("adr/README.md"),
                        new DocumentPath("adr/0001-record.md"),
                        new DocumentPath("adr/superseded/0000-old.md"),
                        new DocumentPath("adr-old/0000-first.md"));
    }

    @Test
    void shouldListFilesThatAreNotMarkdownAndLeaveTheTreeToDropThem() throws IOException {
        write(docs, "openapi.json", "{}");

        assertThat(repository.paths(new Project(root))).containsExactly(new DocumentPath("openapi.json"));
    }

    @Test
    void shouldNotListDirectoriesAsPaths() throws IOException {
        Files.createDirectories(docs.resolve("empty"));

        assertThat(repository.paths(new Project(root))).isEmpty();
    }

    @Test
    void shouldFailWhenADirectoryCannotBeListed() throws IOException {
        Path locked = Files.createDirectories(docs.resolve("locked"));
        forbid(locked);
        try {
            assertThatThrownBy(() -> repository.paths(new Project(root)))
                    .as("a directory that cannot be walked is a failed read, not a silently shorter tree")
                    .isInstanceOf(DocsUnavailableException.class);
        } finally {
            allow(locked);
        }
    }

    @Test
    void shouldReadADocumentsText() throws IOException {
        write(docs, "adr/0001-record.md", markdown());

        assertThat(repository.read(new Project(root), new DocumentPath("adr/0001-record.md")))
                .isEqualTo(ReadDocument.builder()
                        .path(new DocumentPath("adr/0001-record.md"))
                        .text(new Content(markdown()))
                        .build());
    }

    @Test
    void shouldReadAnEmptyFileAsAnEmptyDocument() throws IOException {
        write(docs, "empty.md", "");

        assertThat(repository.read(new Project(root), new DocumentPath("empty.md")))
                .isEqualTo(ReadDocument.builder()
                        .path(new DocumentPath("empty.md"))
                        .text(new EmptyDocument())
                        .build());
    }

    @Test
    void shouldReadAWhitespaceFileAsAnEmptyDocument() throws IOException {
        write(docs, "blank.md", " \n\t\n");

        assertThat(repository.read(new Project(root), new DocumentPath("blank.md")))
                .isEqualTo(ReadDocument.builder()
                        .path(new DocumentPath("blank.md"))
                        .text(new EmptyDocument())
                        .build());
    }

    @Test
    void shouldDropAByteOrderMark() throws IOException {
        write(docs, "bom.md", "﻿# Title\n");

        assertThat(repository.read(new Project(root), new DocumentPath("bom.md")))
                .isEqualTo(ReadDocument.builder()
                        .path(new DocumentPath("bom.md"))
                        .text(new Content("# Title\n"))
                        .build());
    }

    @Test
    void shouldReadAFileThatIsNotUtf8AsUnreadable() throws IOException {
        Files.write(docs.resolve("latin.md"), "café".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(repository.read(new Project(root), new DocumentPath("latin.md")))
                .isEqualTo(UnreadableDocument.builder()
                        .path(new DocumentPath("latin.md"))
                        .reason(new Reason("cannot be read: not valid UTF-8"))
                        .build());
    }

    @Test
    void shouldReadAFileThatCannotBeOpenedAsUnreadableWithTheReason() throws IOException {
        Path locked = write(docs, "locked.md", markdown());
        forbid(locked);
        try {
            Document document = repository.read(new Project(root), new DocumentPath("locked.md"));

            assertThat(document)
                    .isEqualTo(UnreadableDocument.builder()
                            .path(new DocumentPath("locked.md"))
                            .reason(new Reason("cannot be read: AccessDeniedException"))
                            .build());
        } finally {
            allow(locked);
        }
    }

    @Test
    void shouldReadAFileDeletedSinceTheListingAsUnreadable() {
        assertThat(repository.read(new Project(root), new DocumentPath("gone.md")))
                .isEqualTo(UnreadableDocument.builder()
                        .path(new DocumentPath("gone.md"))
                        .reason(new Reason("cannot be read: NoSuchFileException"))
                        .build());
    }
}
