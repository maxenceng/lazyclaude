package dev.maxenceng.lazyclaude.docs.published;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A folder's place under {@code docs/}, `/`-separated; it is the folder's identity across reads, as a {@link DocumentPath} is a document's. */
public record FolderPath(String value) {

    private static final String SEPARATOR = "/";

    public FolderPath {
        Assert.notBlank("folder path", value);
        Assert.matchesPattern("folder path", value, DocumentPath.SHAPE, DocumentPath.SHAPE_IN_WORDS);
    }

    public static FolderPath of(List<String> segments) {
        return new FolderPath(String.join(SEPARATOR, segments));
    }

    public List<String> segments() {
        return List.of(value.split(SEPARATOR));
    }

    public FolderName name() {
        return new FolderName(segments().getLast());
    }
}
