package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ChurnTolerantVisitor;

/** Collects the path of every regular file under {@code docs/}, relative to it and `/`-separated, whatever the file is. */
final class DocumentPathCollector extends ChurnTolerantVisitor {

    private static final String SEPARATOR = "/";

    private final Path docs;
    private final List<DocumentPath> collected = new ArrayList<>();

    DocumentPathCollector(Path docs) {
        this.docs = docs;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        if (attributes.isRegularFile()) {
            collected.add(new DocumentPath(relative(file)));
        }
        return FileVisitResult.CONTINUE;
    }

    /** What was collected, in tree order. */
    List<DocumentPath> collected() {
        return collected.stream().sorted().toList();
    }

    private String relative(Path file) {
        return docs.relativize(file).normalize().toString().replace(file.getFileSystem().getSeparator(), SEPARATOR);
    }
}
