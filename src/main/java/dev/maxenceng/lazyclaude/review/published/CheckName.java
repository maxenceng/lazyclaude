package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The name of a check run, or the context of a commit status; orders as the names do. */
public record CheckName(String value) implements Comparable<CheckName> {

    public CheckName {
        Assert.notBlank("check name", value);
    }

    @Override
    public int compareTo(CheckName other) {
        return value.compareTo(other.value);
    }
}
