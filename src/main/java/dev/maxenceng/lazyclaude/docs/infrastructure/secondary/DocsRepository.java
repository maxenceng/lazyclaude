package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.docs.domain.DocsPort;
import dev.maxenceng.lazyclaude.docs.domain.DocsUnavailableException;
import dev.maxenceng.lazyclaude.docs.domain.Document;
import dev.maxenceng.lazyclaude.docs.domain.ReadDocument;
import dev.maxenceng.lazyclaude.docs.domain.UnreadableDocument;
import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.docs.published.DocumentText;
import dev.maxenceng.lazyclaude.docs.published.Reason;
import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.Utf8Text;

/** Reads {@code docs/**}: every file's path, and a document's text as UTF-8 without a byte order mark. */
public final class DocsRepository implements DocsPort {

    private static final Path DOCS = Path.of("docs");
    private static final String SEPARATOR = "/";
    private static final String UNREADABLE = "cannot be read: %s";
    private static final String NOT_UTF8 = "not valid UTF-8";

    @Override
    public List<DocumentPath> paths(Project project) {
        Path docs = project.root().resolve(DOCS);
        if (!Files.isDirectory(docs)) {
            return List.of();
        }
        List<DocumentPath> paths = new ArrayList<>();
        try {
            Files.walkFileTree(docs, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (attributes.isRegularFile()) {
                        paths.add(new DocumentPath(relative(docs, file)));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException failure) throws IOException {
                    if (failure instanceof NoSuchFileException) {
                        return FileVisitResult.CONTINUE;
                    }
                    throw failure;
                }
            });
        } catch (IOException e) {
            throw new DocsUnavailableException(docs, e);
        }
        return List.copyOf(paths);
    }

    @Override
    public Document read(Project project, DocumentPath path) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(project.root().resolve(DOCS).resolve(path.value()));
        } catch (IOException e) {
            return unreadable(path, UNREADABLE.formatted(e.getClass().getSimpleName()));
        }
        return Utf8Text.decode(bytes)
                .<Document>map(text -> ReadDocument.builder()
                        .path(path)
                        .text(DocumentText.of(text))
                        .build())
                .orElseGet(() -> unreadable(path, UNREADABLE.formatted(NOT_UTF8)));
    }

    private static String relative(Path docs, Path file) {
        return docs.relativize(file).normalize().toString().replace(file.getFileSystem().getSeparator(), SEPARATOR);
    }

    private static UnreadableDocument unreadable(DocumentPath path, String reason) {
        return UnreadableDocument.builder().path(path).reason(new Reason(reason)).build();
    }
}
