package dev.maxenceng.lazyclaude.docs.published;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A file's place under {@code docs/}, `/`-separated; it is a document's identity across reads. The
 * port names every file it finds this way, and {@link #listed()} decides which of them are documents.
 */
public record DocumentPath(String value) implements Comparable<DocumentPath> {

    private static final String SEPARATOR = "/";
    private static final String MARKDOWN = ".md";
    private static final String BACKLOG = "backlog";
    private static final String README = "README.md";
    static final Pattern SHAPE = Pattern.compile("(?!\\.\\.?(/|$))[^/]+(/(?!\\.\\.?(/|$))[^/]+)*");
    static final String SHAPE_IN_WORDS = "folder/folder/file, without . or ..";

    public DocumentPath {
        Assert.notBlank("document path", value);
        Assert.matchesPattern("document path", value, SHAPE, SHAPE_IN_WORDS);
    }

    public List<String> segments() {
        return List.of(value.split(SEPARATOR));
    }

    public String fileName() {
        return segments().getLast();
    }

    /** Whether the docs tree holds it: a markdown file that is not under `docs/backlog/`, which the board owns. */
    public boolean listed() {
        return fileName().endsWith(MARKDOWN) && !segments().getFirst().equals(BACKLOG);
    }

    /**
     * Orders as the tree and the site do, level by level: a folder's {@code README.md} first, then its
     * other files by name, then its subfolders by name. Names compare segment by segment, so
     * {@code adr/x.md} comes before {@code adr-old/y.md}, because {@code adr} comes before {@code adr-old}.
     */
    @Override
    public int compareTo(DocumentPath other) {
        List<String> mine = segments();
        List<String> theirs = other.segments();
        return IntStream.range(0, Math.min(mine.size(), theirs.size()))
                .filter(level -> !mine.get(level).equals(theirs.get(level)))
                .map(level -> atLevel(level).compare(mine, theirs))
                .findFirst()
                .orElseGet(() -> Integer.compare(mine.size(), theirs.size()));
    }

    private static Comparator<List<String>> atLevel(int level) {
        return Comparator.<List<String>, Boolean>comparing(segments -> !isReadmeAt(segments, level))
                .thenComparing(segments -> isFolderAt(segments, level))
                .thenComparing(segments -> segments.get(level));
    }

    private static boolean isReadmeAt(List<String> segments, int level) {
        return !isFolderAt(segments, level) && segments.get(level).equals(README);
    }

    private static boolean isFolderAt(List<String> segments, int level) {
        return segments.size() > level + 1;
    }
}
