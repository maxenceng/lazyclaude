package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import dev.maxenceng.lazyclaude.board.domain.Backlog;
import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.Status;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.TicketId;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Reads `docs/backlog/*.md`: YAML frontmatter for the fields, the first `# ` line for the title. */
public final class MarkdownBacklog implements Backlog {

    private static final String FENCE_OPEN = "---";
    private static final String BYTE_ORDER_MARK = "\uFEFF";
    private static final Pattern ID_LINE = Pattern.compile("(?m)^id:[ \\t]*\\S");

    @Override
    public Board read(Project project) {
        List<Ticket> tickets = new ArrayList<>();
        List<UnreadableTicket> unreadable = new ArrayList<>();
        Set<TicketId> seen = new HashSet<>();
        for (Path file : markdownFiles(project.root().resolve("docs").resolve("backlog"))) {
            String name = file.getFileName().toString();
            Entry entry = parse(file);
            switch (entry) {
                case Entry.Skipped skipped -> {}
                case Entry.Refused refused -> unreadable.add(new UnreadableTicket(name, refused.reason()));
                case Entry.Read read when !seen.add(read.ticket().id()) ->
                    unreadable.add(new UnreadableTicket(name, "id " + read.ticket().id().value() + " is already used"));
                case Entry.Read read -> tickets.add(read.ticket());
            }
        }
        return Board.of(tickets, unreadable);
    }

    private static List<Path> markdownFiles(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".md"))
                    .filter(file -> !file.getFileName().toString().startsWith("_"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list " + directory, e);
        }
    }

    private static Entry parse(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            return new Entry.Refused("cannot be read: " + e.getClass().getSimpleName());
        }
        Optional<String> decoded = utf8(bytes);
        String content = decoded.orElseGet(() -> new String(bytes, StandardCharsets.ISO_8859_1));
        List<String> lines = content.lines().toList();
        if (lines.isEmpty() || !lines.getFirst().strip().equals(FENCE_OPEN)) {
            return new Entry.Skipped();
        }
        int closing = closingFence(lines);
        String frontmatter = String.join("\n", lines.subList(1, closing < 0 ? lines.size() : closing));
        if (decoded.isEmpty()) {
            return hasId(frontmatter) ? new Entry.Refused("cannot be read: not valid UTF-8") : new Entry.Skipped();
        }
        if (closing < 0) {
            return hasId(frontmatter) ? new Entry.Refused("frontmatter has no closing ---") : new Entry.Skipped();
        }
        Object loaded;
        try {
            loaded = new Load(LoadSettings.builder().build()).loadFromString(frontmatter);
        } catch (YamlEngineException e) {
            return hasId(frontmatter)
                    ? new Entry.Refused("frontmatter is not valid YAML: " + firstLine(e.getMessage()))
                    : new Entry.Skipped();
        }
        if (!(loaded instanceof Map<?, ?> fields) || fields.get("id") == null) {
            return new Entry.Skipped();
        }
        return ticket(fields, lines.subList(closing + 1, lines.size()));
    }

    private static Entry ticket(Map<?, ?> fields, List<String> body) {
        try {
            Object status = fields.get("status");
            if (status == null) {
                return new Entry.Refused("status is missing");
            }
            return new Entry.Read(Ticket.builder()
                    .id(TicketId.of(String.valueOf(fields.get("id"))))
                    .title(title(body).orElse(null))
                    .status(Status.parse(String.valueOf(status)))
                    .context(text(fields.get("context")))
                    .type(text(fields.get("type")))
                    .build());
        } catch (IllegalArgumentException e) {
            return new Entry.Refused(e.getMessage());
        }
    }

    private static Optional<String> utf8(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return Optional.of(text.startsWith(BYTE_ORDER_MARK) ? text.substring(1) : text);
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    private static int closingFence(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).strip().equals(FENCE_OPEN)) {
                return i;
            }
        }
        return -1;
    }

    private static Optional<String> title(List<String> body) {
        boolean fenced = false;
        for (String line : body) {
            if (line.startsWith("```") || line.startsWith("~~~")) {
                fenced = !fenced;
            } else if (!fenced && line.startsWith("# ")) {
                return Optional.of(line.substring(2));
            }
        }
        return Optional.empty();
    }

    private static boolean hasId(String frontmatter) {
        return ID_LINE.matcher(frontmatter).find();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstLine(String message) {
        return message == null ? "unreadable" : message.lines().findFirst().orElse("unreadable");
    }

    private sealed interface Entry {
        record Skipped() implements Entry {}

        record Refused(String reason) implements Entry {}

        record Read(Ticket ticket) implements Entry {}
    }
}
