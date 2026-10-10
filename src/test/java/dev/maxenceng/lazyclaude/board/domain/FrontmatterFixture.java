package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.blockedByEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.createdEntry;

import java.util.List;

public final class FrontmatterFixture {

    private FrontmatterFixture() {}

    public static Frontmatter frontmatter() {
        return new Frontmatter(List.of());
    }

    public static Frontmatter datedFrontmatter() {
        return new Frontmatter(List.of(createdEntry(), blockedByEntry()));
    }
}
