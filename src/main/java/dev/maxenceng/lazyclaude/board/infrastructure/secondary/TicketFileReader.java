package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;
import dev.maxenceng.lazyclaude.board.published.Body;
import dev.maxenceng.lazyclaude.board.published.FileName;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.Reason;
import dev.maxenceng.lazyclaude.board.published.TicketBody;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.Utf8Text;

/**
 * Reads one backlog file's bytes: YAML frontmatter for the fields, everything after the closing `---` for the body. A file is an
 * entry only when its frontmatter claims an `id`. The root checkout, a worktree and a branch all hold the same kind of file, so all
 * three read it here.
 */
final class TicketFileReader {

    private static final String NOT_UTF_8 = "cannot be read: not valid UTF-8";
    private static final String FENCE = "---";
    private static final String LINE_BREAK = "\n";
    private static final Pattern ID_LINE = Pattern.compile("(?m)^id:[ \\t]*\\S");
    private static final String NO_DETAIL = "unreadable";

    private final TicketFrontmatterMapper frontmatterMapper = new TicketFrontmatterMapper();

    Optional<BacklogEntry> read(FileName name, byte[] bytes) {
        Optional<String> decoded = Utf8Text.decode(bytes);
        List<String> lines = decoded.orElseGet(() -> new String(bytes, StandardCharsets.ISO_8859_1))
                .lines()
                .toList();
        if (lines.isEmpty() || !lines.getFirst().strip().equals(FENCE)) {
            return Optional.empty();
        }
        OptionalInt closing = closingFence(lines);
        String frontmatter = String.join(LINE_BREAK, lines.subList(1, closing.orElse(lines.size())));
        if (decoded.isEmpty()) {
            return refusedIfClaimed(name, frontmatter, NOT_UTF_8);
        }
        if (closing.isEmpty()) {
            return refusedIfClaimed(name, frontmatter, "frontmatter has no closing %s".formatted(FENCE));
        }
        TicketFrontmatter fields;
        try {
            fields = frontmatterMapper.read(frontmatter);
        } catch (YamlEngineException e) {
            return refusedIfClaimed(
                    name, frontmatter, "frontmatter is not valid YAML: %s".formatted(firstLine(e.getMessage())));
        }
        if (!fields.claimsATicket()) {
            return Optional.empty();
        }
        List<String> body = lines.subList(closing.getAsInt() + 1, lines.size());
        return Optional.of(entry(name, fields, body(String.join(LINE_BREAK, body))));
    }

    /** Whitespace renders as a blank, so a body of only whitespace is as good as none. */
    private static TicketBody body(String text) {
        return text.isBlank() ? new NoBody() : new Body(text);
    }

    /** Reads the file at {@code file}, named by its last element. */
    Optional<BacklogEntry> read(Path file) {
        FileName name = new FileName(file.getFileName().toString());
        try {
            return read(name, Files.readAllBytes(file));
        } catch (IOException e) {
            return Optional.of(refused(name, "cannot be read: %s".formatted(e.getClass().getSimpleName())));
        }
    }

    private static BacklogEntry entry(FileName name, TicketFrontmatter fields, TicketBody body) {
        return fields.entry(name, body);
    }

    static UnreadableTicket refused(FileName name, String reason) {
        return UnreadableTicket.builder().file(name).reason(new Reason(reason)).build();
    }

    /** Frontmatter that cannot be parsed is only an entry when a line of it still claims an `id`. */
    private static Optional<BacklogEntry> refusedIfClaimed(FileName name, String frontmatter, String reason) {
        return ID_LINE.matcher(frontmatter).find() ? Optional.of(refused(name, reason)) : Optional.empty();
    }

    private static OptionalInt closingFence(List<String> lines) {
        return IntStream.range(1, lines.size())
                .filter(index -> lines.get(index).strip().equals(FENCE))
                .findFirst();
    }

    private static String firstLine(String message) {
        return message == null ? NO_DETAIL : message.lines().findFirst().orElse(NO_DETAIL);
    }
}
