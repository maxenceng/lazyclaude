package dev.maxenceng.lazyclaude.docs.published;

import java.util.List;
import java.util.regex.Pattern;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A document's place under {@code docs/}, `/`-separated; it is the document's identity across reads. */
public record DocumentPath(String value) implements Comparable<DocumentPath> {

    private static final String SEPARATOR = "/";
    private static final String MARKDOWN = ".md";
    private static final String BACKLOG = "backlog";
    private static final Pattern SHAPE = Pattern.compile("[^/]+(/[^/]+)*");
    private static final String SHAPE_IN_WORDS = "folder/folder/file";

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

    /** Whether the Docs panel shows it: a markdown file that is not under `docs/backlog/`, which the board owns. */
    public boolean listed() {
        return fileName().endsWith(MARKDOWN) && !segments().getFirst().equals(BACKLOG);
    }

    @Override
    public int compareTo(DocumentPath other) {
        return value.compareTo(other.value);
    }
}
