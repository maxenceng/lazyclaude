package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;

/** Walks a tree while files come and go: one that vanished since its folder was listed is skipped, any other failure ends the walk. */
public abstract class ChurnTolerantVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException failure) throws IOException {
        if (failure instanceof NoSuchFileException) {
            return FileVisitResult.CONTINUE;
        }
        throw failure;
    }
}
