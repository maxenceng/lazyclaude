package dev.maxenceng.lazyclaude.docs.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A folder's name within its parent folder; orders as the names do. */
public record FolderName(String value) implements Comparable<FolderName> {

    public FolderName {
        Assert.notBlank("folder", value);
    }

    @Override
    public int compareTo(FolderName other) {
        return value.compareTo(other.value);
    }
}
