package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import dev.maxenceng.lazyclaude.docs.published.DocumentPath;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.TreeScope;

/** Every directory under {@code docs/} but {@code docs/backlog/}, and the files the Docs panel lists. */
final class DocsScope implements TreeScope {

    private static final String BACKLOG = "backlog";
    private static final String SEPARATOR = "/";

    private final Path docs;
    private final Path backlog;

    DocsScope(Path docs) {
        this.docs = docs;
        this.backlog = docs.resolve(BACKLOG);
    }

    @Override
    public Path root() {
        return docs;
    }

    @Override
    public boolean descends(Path directory) {
        return !directory.equals(backlog);
    }

    @Override
    public boolean counts(Path entry) {
        return !entry.startsWith(backlog) && listed(entry);
    }

    private boolean listed(Path entry) {
        String relative = StreamSupport.stream(docs.relativize(entry).spliterator(), false)
                .map(Path::toString)
                .collect(Collectors.joining(SEPARATOR));
        return new DocumentPath(relative).listed();
    }
}
