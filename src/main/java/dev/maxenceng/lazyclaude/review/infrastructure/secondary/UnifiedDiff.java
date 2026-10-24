package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.domain.DiffFile;
import dev.maxenceng.lazyclaude.review.domain.Hunk;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiff;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.published.AddedFile;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.ContextLine;
import dev.maxenceng.lazyclaude.review.published.DeletedFile;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineContent;
import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.DiffPath;
import dev.maxenceng.lazyclaude.review.published.FileStatus;
import dev.maxenceng.lazyclaude.review.published.HunkHeader;
import dev.maxenceng.lazyclaude.review.published.ModifiedFile;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;
import dev.maxenceng.lazyclaude.review.published.RenamedFile;

/** Reads the unified diff {@code gh pr diff} prints into a {@link PullRequestDiff}. */
final class UnifiedDiff {

    private static final String NO_LINE_YET = "";
    private static final String SOURCE = "gh pr diff";
    private static final String UNPARSED = "%s does not parse: %s";
    private static final Pattern FILE_START = Pattern.compile("(?m)^(?=diff --git )");
    private static final Pattern HUNK_START = Pattern.compile("(?m)^(?=@@ )");
    private static final Pattern LINE_BREAK = Pattern.compile("\n");
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d{1,9})(?:,\\d+)? \\+(\\d{1,9})(?:,\\d+)? @@.*");
    private static final String FIRST_HUNK = "\n@@ ";
    private static final String FILE_LINE = "diff --git ";
    private static final String NO_NEWLINE_MARKER = "\\";
    private static final String RENAME_FROM = "rename from ";
    private static final String RENAME_TO = "rename to ";
    private static final String NEW_FILE = "new file mode ";
    private static final String DELETED_FILE = "deleted file mode ";
    private static final String OLD_SIDE = "a/";
    private static final String NEW_SIDE_GAP = " b/";
    private static final int PATHS_ON_THE_LINE = 2;
    private static final int OLD_START = 1;
    private static final int NEW_START = 2;

    private UnifiedDiff() {}

    /** @throws PullRequestsUnavailableException saying what {@code gh pr diff} printed that is not a unified diff */
    static PullRequestDiff parse(String text) {
        return new PullRequestDiff(FILE_START.splitAsStream(text).filter(section -> !section.isBlank()).map(UnifiedDiff::file).toList());
    }

    private static DiffFile file(String section) {
        int firstHunk = section.indexOf(FIRST_HUNK);
        String head = firstHunk < 0 ? section : section.substring(0, firstHunk);
        Stream<String> hunks = firstHunk < 0 ? Stream.empty() : HUNK_START.splitAsStream(section.substring(firstHunk + 1));
        List<String> headLines = LINE_BREAK.splitAsStream(head).toList();
        return DiffFile.builder()
                .path(named(after(headLines, RENAME_TO).orElseGet(() -> pathOf(headLines.getFirst())), headLines.getFirst()))
                .status(statusOf(headLines))
                .hunks(hunks.map(UnifiedDiff::hunk).toList())
                .build();
    }

    private static FileStatus statusOf(List<String> head) {
        return after(head, RENAME_FROM)
                .<FileStatus>map(from -> new RenamedFile(named(from, head.getFirst())))
                .or(() -> after(head, NEW_FILE).map(_ -> new AddedFile()))
                .or(() -> after(head, DELETED_FILE).map(_ -> new DeletedFile()))
                .orElseGet(ModifiedFile::new);
    }

    /** @throws PullRequestsUnavailableException when the header of the file starting at {@code fileLine} names no path */
    private static DiffPath named(String path, String fileLine) {
        if (path.isBlank()) {
            throw new PullRequestsUnavailableException(UNPARSED.formatted(SOURCE, fileLine));
        }
        return new DiffPath(path);
    }

    /** What follows {@code prefix} on the first header line that starts with it. */
    private static Optional<String> after(List<String> head, String prefix) {
        return head.stream().filter(line -> line.startsWith(prefix)).findFirst().map(line -> line.substring(prefix.length()));
    }

    // deferred: a path git quotes (one with non-ASCII characters) reads with its quotes and escapes — close it when a ticket's diff names such a file.
    private static String pathOf(String fileLine) {
        String paths = fileLine.substring(FILE_LINE.length());
        if (paths.length() < OLD_SIDE.length() + NEW_SIDE_GAP.length()) {
            throw new PullRequestsUnavailableException(UNPARSED.formatted(SOURCE, fileLine));
        }
        int length = (paths.length() - NEW_SIDE_GAP.length() - OLD_SIDE.length()) / PATHS_ON_THE_LINE;
        return paths.substring(OLD_SIDE.length(), OLD_SIDE.length() + length);
    }

    private static Hunk hunk(String section) {
        List<String> lines = LINE_BREAK.splitAsStream(section).toList();
        Matcher header = HUNK_HEADER.matcher(lines.getFirst());
        if (!header.matches()) {
            throw new PullRequestsUnavailableException(UNPARSED.formatted(SOURCE, lines.getFirst()));
        }
        ScannedDiffLine before = new ScannedDiffLine(NO_LINE_YET, Integer.parseInt(header.group(OLD_START)), Integer.parseInt(header.group(NEW_START)));
        return Hunk.builder()
                .header(new HunkHeader(lines.getFirst()))
                .lines(lines.stream()
                        .skip(1)
                        .filter(line -> !line.startsWith(NO_NEWLINE_MARKER))
                        .gather(Gatherers.scan(() -> before, ScannedDiffLine::then))
                        .map(UnifiedDiff::line)
                        .toList())
                .build();
    }

    private static DiffLine line(ScannedDiffLine scanned) {
        DiffLineContent content = new DiffLineContent(scanned.content());
        DiffLineNumber oldNumber = new DiffLineNumber(scanned.nextOld() - 1);
        DiffLineNumber newNumber = new DiffLineNumber(scanned.nextNew() - 1);
        if (scanned.added()) {
            return AddedLine.builder().content(content).newNumber(newNumber).build();
        }
        if (scanned.removed()) {
            return RemovedLine.builder().content(content).oldNumber(oldNumber).build();
        }
        return ContextLine.builder().content(content).oldNumber(oldNumber).newNumber(newNumber).build();
    }
}
