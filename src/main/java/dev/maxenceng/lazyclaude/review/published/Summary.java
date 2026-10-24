package dev.maxenceng.lazyclaude.review.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** What the review says as a whole: typed on the review screen, or read as an adopted pending review's body, and sent as its body by the submit. */
public record Summary(String value) {

    public Summary {
        Assert.notNull("summary", value);
    }

    /** Whether it says nothing: empty, or only white space. */
    public boolean isBlank() {
        return value.isBlank();
    }

    /** The first line, which is all the Review panel has room for. */
    public String firstLine() {
        return value.lines().findFirst().orElse("");
    }
}
