package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.nio.file.Path;
import java.util.Set;

final class MarkdownScope implements TreeScope {

    private final Path root;
    private final Set<Path> skipped;

    MarkdownScope(Path root, Set<Path> skipped) {
        this.root = root;
        this.skipped = skipped;
    }

    @Override
    public Path root() {
        return root;
    }

    @Override
    public boolean descends(Path directory) {
        return !skipped.contains(directory);
    }

    @Override
    public boolean counts(Path entry) {
        return entry.getFileName().toString().endsWith(".md") && skipped.stream().noneMatch(entry::startsWith);
    }
}
