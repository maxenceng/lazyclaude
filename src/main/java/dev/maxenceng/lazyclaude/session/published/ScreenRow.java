package dev.maxenceng.lazyclaude.session.published;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** One row of the terminal screen, left to right. */
public record ScreenRow(List<StyledRun> runs) {

    public ScreenRow {
        Assert.notNull("runs", runs);
    }
}
