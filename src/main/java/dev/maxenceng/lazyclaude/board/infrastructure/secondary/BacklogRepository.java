package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads `docs/backlog/*.md`: YAML frontmatter for the fields, everything after the closing `---` for the body.
 * A file is an entry only when its frontmatter claims an `id`.
 */
public final class BacklogRepository implements BacklogPort {

    private static final Path BACKLOG = Path.of("docs", "backlog");
    private static final String MARKDOWN = ".md";
    private static final String HIDDEN_PREFIX = "_";

    private final TicketFileReader reader = new TicketFileReader();

    @Override
    public List<BacklogEntry> entries(Project project) {
        return markdownFiles(project.root().resolve(BACKLOG)).stream()
                .map(reader::read)
                .flatMap(Optional::stream)
                .toList();
    }

    private static List<Path> markdownFiles(Path backlog) {
        if (!Files.isDirectory(backlog)) {
            throw new BacklogNotFoundException(backlog);
        }
        try (Stream<Path> files = Files.list(backlog)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> name(file).endsWith(MARKDOWN))
                    .filter(file -> !name(file).startsWith(HIDDEN_PREFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new BacklogUnavailableException(backlog, e);
        }
    }

    private static String name(Path file) {
        return file.getFileName().toString();
    }
}
