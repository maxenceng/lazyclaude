package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogNotFoundException;
import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.Status;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.TicketId;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Reads `docs/backlog/*.md`: YAML frontmatter for the fields, the first `# ` line outside a code
 * fence for the title. A file is an entry only when its frontmatter claims an `id`.
 */
public final class BacklogRepository implements BacklogPort {

    private static final Path BACKLOG = Path.of("docs", "backlog");
    private static final String MARKDOWN = ".md";
    private static final String HIDDEN_PREFIX = "_";
    private static final String FENCE = "---";
    private static final String TITLE_PREFIX = "# ";
    private static final List<String> CODE_FENCES = List.of("```", "~~~");
    private static final String BYTE_ORDER_MARK = "﻿";
    private static final Pattern ID_LINE = Pattern.compile("(?m)^id:[ \\t]*\\S");
    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String CONTEXT = "context";
    private static final String TYPE = "type";
    private static final String NO_DETAIL = "unreadable";

    @Override
    public List<BacklogEntry> entries(Project project) {
        return markdownFiles(project.root().resolve(BACKLOG)).stream()
                .map(BacklogRepository::entry)
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

    private static Optional<BacklogEntry> entry(Path file) {
        String name = name(file);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            return refused(name, "cannot be read: %s".formatted(e.getClass().getSimpleName()));
        }
        Optional<String> decoded = utf8(bytes);
        List<String> lines = decoded.orElseGet(() -> new String(bytes, StandardCharsets.ISO_8859_1))
                .lines()
                .toList();
        if (lines.isEmpty() || !lines.getFirst().strip().equals(FENCE)) {
            return Optional.empty();
        }
        int closing = closingFence(lines);
        String frontmatter = String.join("\n", lines.subList(1, closing < 0 ? lines.size() : closing));
        if (decoded.isEmpty()) {
            return refusedIfClaimed(name, frontmatter, "cannot be read: not valid UTF-8");
        }
        if (closing < 0) {
            return refusedIfClaimed(name, frontmatter, "frontmatter has no closing %s".formatted(FENCE));
        }
        Map<?, ?> fields;
        try {
            fields = new Load(LoadSettings.builder().build()).loadFromString(frontmatter) instanceof Map<?, ?> loaded
                    ? loaded
                    : Map.of();
        } catch (YamlEngineException e) {
            return refusedIfClaimed(
                    name, frontmatter, "frontmatter is not valid YAML: %s".formatted(firstLine(e.getMessage())));
        }
        if (fields.get(ID) == null) {
            return Optional.empty();
        }
        return Optional.of(ticket(name, fields, lines.subList(closing + 1, lines.size())));
    }

    private static BacklogEntry ticket(String name, Map<?, ?> fields, List<String> body) {
        Object status = fields.get(STATUS);
        if (status == null) {
            return new UnreadableTicket(name, "status is missing");
        }
        try {
            return new TicketFile(
                    name,
                    Ticket.builder()
                            .id(new TicketId(String.valueOf(fields.get(ID))))
                            .title(title(body).orElse(null))
                            .status(Status.parse(String.valueOf(status)))
                            .context(text(fields.get(CONTEXT)))
                            .type(text(fields.get(TYPE)))
                            .build());
        } catch (DomainException e) {
            return new UnreadableTicket(name, e.getMessage());
        }
    }

    private static Optional<BacklogEntry> refused(String name, String reason) {
        return Optional.of(new UnreadableTicket(name, reason));
    }

    /** Frontmatter that cannot be parsed is only an entry when a line of it still claims an `id`. */
    private static Optional<BacklogEntry> refusedIfClaimed(String name, String frontmatter, String reason) {
        return ID_LINE.matcher(frontmatter).find() ? refused(name, reason) : Optional.empty();
    }

    private static String name(Path file) {
        return file.getFileName().toString();
    }

    private static Optional<String> utf8(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return Optional.of(text.startsWith(BYTE_ORDER_MARK) ? text.substring(BYTE_ORDER_MARK.length()) : text);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    private static int closingFence(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).strip().equals(FENCE)) {
                return i;
            }
        }
        return -1;
    }

    private static Optional<String> title(List<String> body) {
        boolean inCode = false;
        for (String line : body) {
            if (CODE_FENCES.stream().anyMatch(line::startsWith)) {
                inCode = !inCode;
            } else if (!inCode && line.startsWith(TITLE_PREFIX)) {
                return Optional.of(line.substring(TITLE_PREFIX.length()));
            }
        }
        return Optional.empty();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstLine(String message) {
        return message == null ? NO_DETAIL : message.lines().findFirst().orElse(NO_DETAIL);
    }
}
