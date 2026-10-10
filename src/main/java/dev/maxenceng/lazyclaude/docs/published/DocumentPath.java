package dev.maxenceng.lazyclaude.docs.published;

import java.util.List;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A file's place under {@code docs/}, `/`-separated; it is a document's identity across reads. The
 * port names every file it finds this way, and {@link #listed()} decides which of them are documents.
 */
public record DocumentPath(String value) implements Comparable<DocumentPath> {

    private static final String SEPARATOR = "/";
    private static final String MARKDOWN = ".md";
    private static final String BACKLOG = "backlog";
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

    /** Orders as the tree does, segment by segment: {@code adr/x.md} before {@code adr-old/y.md}, because {@code adr} comes before {@code adr-old}. */
    @Override
    public int compareTo(DocumentPath other) {
        List<String> mine = segments();
        List<String> theirs = other.segments();
        for (int index = 0; index < Math.min(mine.size(), theirs.size()); index++) {
            int order = mine.get(index).compareTo(theirs.get(index));
            if (order != 0) {
                return order;
            }
        }
        return Integer.compare(mine.size(), theirs.size());
    }
}
