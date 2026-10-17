package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Puts every directory of a scope under a {@link WatchService} and records each one's key by its path. A directory the scope does
 * not descend into, or that vanished before it could be registered, is skipped with what is beneath it.
 */
final class TreeRegistration extends ChurnTolerantVisitor {

    private final WatchService service;
    private final DirectoryRegistrar registrar;
    private final TreeScope scope;
    private final Map<Path, WatchKey> watched;

    TreeRegistration(WatchService service, DirectoryRegistrar registrar, TreeScope scope, Map<Path, WatchKey> watched) {
        this.service = service;
        this.registrar = registrar;
        this.scope = scope;
        this.watched = watched;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
        if (!scope.descends(directory)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        try {
            watched.put(directory, registrar.register(service, directory));
        } catch (NoSuchFileException vanished) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }
}
