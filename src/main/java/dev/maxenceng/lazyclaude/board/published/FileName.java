package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A backlog file's name within `docs/backlog/`; orders as the names do. */
public record FileName(String value) implements Comparable<FileName> {

    public FileName {
        Assert.notBlank("file", value);
    }

    @Override
    public int compareTo(FileName other) {
        return value.compareTo(other.value);
    }
}
