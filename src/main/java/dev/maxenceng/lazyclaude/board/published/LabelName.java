package dev.maxenceng.lazyclaude.board.published;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** A label of a ticket's issue that the board does not model, shown with the ticket as written. */
public record LabelName(String value) {

    public LabelName {
        Assert.notBlank("label", value);
    }
}
