package dev.maxenceng.lazyclaude.docs.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Stream;

public final class DocsFixture {

    private static final String NO_ACCESS = "---------";

    private DocsFixture() {}

    public static Path docsIn(Path root) throws IOException {
        return Files.createDirectories(root.resolve("docs"));
    }

    public static Path write(Path docs, String relative, String text) throws IOException {
        Path file = docs.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text);
    }

    public static void forbid(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(NO_ACCESS));
    }

    public static void allow(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    public static void deleteTree(Path tree) throws IOException {
        try (Stream<Path> paths = Files.walk(tree)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    public static String markdown() {
        return "# Context map\n\nEach context is a package.\n";
    }
}
