package dev.maxenceng.lazyclaude.docs.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A folder's name within its parent folder. */
public record FolderName(String value) {

    public FolderName {
        Assert.notBlank("folder", value);
    }
}
