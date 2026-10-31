package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/** What a scope holds right now, by path: the modification time and size of each entry it counts, so two reads differ when the tree changed. */
final class TreeStamps extends ChurnTolerantVisitor {

    private static final String STAMP = "%s/%d";

    private final TreeScope scope;
    private final Map<String, String> stamps = new HashMap<>();

    private TreeStamps(TreeScope scope) {
        this.scope = scope;
    }

    /** The stamps of {@code scope}, or {@code previous} when the tree cannot be walked, so a failed walk is not mistaken for a change. */
    static Map<String, String> of(TreeScope scope, Map<String, String> previous) {
        TreeStamps walk = new TreeStamps(scope);
        try {
            Files.walkFileTree(scope.root(), walk);
        } catch (IOException unreadable) {
            return previous;
        }
        return walk.stamps;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
        if (!scope.descends(directory)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        stamp(directory, attributes);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
        if (scope.counts(file)) {
            stamp(file, attributes);
        }
        return FileVisitResult.CONTINUE;
    }

    private void stamp(Path path, BasicFileAttributes attributes) {
        stamps.put(path.toString(), STAMP.formatted(attributes.lastModifiedTime(), attributes.size()));
    }
}
